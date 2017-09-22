package uk.ac.warwick.tabula.services.timetables

import dispatch.classic._
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.property.{Categories, DateProperty, RRule}
import net.fortuna.ical4j.model.{Parameter, Property}
import org.apache.commons.codec.digest.DigestUtils
import org.joda.time.{DateTime, DateTimeZone}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.tabula.services.UserLookupService.UniversityId
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.{AutowiringCacheStrategyComponent, CacheStrategyComponent}
import uk.ac.warwick.tabula.services.timetables.CelcatHttpTimetableFetchingService._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, DateFormats}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.parsing.json.JSON

trait CelcatConfiguration {
	val wbsConfiguration: CelcatDepartmentConfiguration
	val cacheEnabled: Boolean
}

trait CelcatConfigurationComponent {
	val celcatConfiguration: CelcatConfiguration
}

case class CelcatDepartmentConfiguration(
	baseUri: String,
	excludedEventTypes: Seq[TimetableEventType] = Nil,
	enabledFn: () => Boolean = { () => true },
	credentials: Credentials
) {
	def enabled: Boolean = enabledFn()
}

trait AutowiringCelcatConfigurationComponent extends CelcatConfigurationComponent {
	val celcatConfiguration = new AutowiringCelcatConfiguration

	class AutowiringCelcatConfiguration extends CelcatConfiguration with AutowiringFeaturesComponent {

		val wbsConfiguration = CelcatDepartmentConfiguration(
			baseUri = "https://rimmer.wbs.ac.uk",
			enabledFn = { () => features.celcatTimetablesWBS },
			credentials = Credentials(Wire.property("${celcat.fetcher.ib.username}"), Wire.property("${celcat.fetcher.ib.password}"))
		)

		val cacheEnabled = true
	}
}

trait CelcatHttpTimetableFetchingServiceComponent extends StaffAndStudentTimetableFetchingServiceComponent {
	self: CelcatConfigurationComponent =>

	lazy val timetableFetchingService = CelcatHttpTimetableFetchingService(celcatConfiguration)
}

object CelcatHttpTimetableFetchingService {
	val cacheName = "CelcatTimetableLists"
	val expectedModuleCodeLength = 5

	def apply(celcatConfiguration: CelcatConfiguration): StudentTimetableFetchingService with StaffTimetableFetchingService = {
		val delegate =
			new CelcatHttpTimetableFetchingService(celcatConfiguration)
				with AutowiringUserLookupComponent
				with AutowiringTermServiceComponent
				with AutowiringCacheStrategyComponent
				with WAI2GoHttpLocationFetchingServiceComponent
				with AutowiringWAI2GoConfigurationComponent
				with AutowiringModuleAndDepartmentServiceComponent
				with AutowiringDispatchHttpClientComponent

		if (celcatConfiguration.cacheEnabled) {
			new CachedStaffAndStudentTimetableFetchingService(delegate, cacheName)
		} else {
			delegate
		}
	}

	private def parseModuleCode(event: VEvent): Option[String] = {
		val summary = Option(event.getSummary).fold("") { _.getValue }
		summary.maybeText.collect { case r"([A-Za-z]{2}[0-9][0-9A-Za-z]{2})${m}.*" => m.toUpperCase }
	}

	def parseVEvent(
		event: VEvent,
		allStaff: Map[UniversityId, CelcatStaffInfo],
		config: CelcatDepartmentConfiguration,
		locationFetchingService: LocationFetchingService,
		moduleMap: Map[String, Module],
		userLookup: UserLookupService
	)(implicit termService: TermService): Option[TimetableEvent] = {
		val summary = Option(event.getSummary).fold("") { _.getValue }
		val categories =
			Option(event.getProperty(Property.CATEGORIES))
				.collect { case c: Categories => c }
				.map { c => c.getCategories.iterator().asScala.collect { case s: String => s }.filter { _.hasText }.toList }
				.getOrElse(Nil)

		val eventType = categories match {
			case singleCategory :: Nil => TimetableEventType(singleCategory)

			case cats if cats.exists { c => TimetableEventType(c).core } =>
				cats.find { c => TimetableEventType(c).core }.map { c => TimetableEventType(c) }.get

			case _ =>	summary.split(" - ", 2) match {
				case Array(t, _) => TimetableEventType(t)
				case Array(s) => TimetableEventType(s)
			}
		}

		if (config.excludedEventTypes.contains(eventType) || eventType.code.toLowerCase.contains("on tabula")) None
		else {
			// Convert date/time to academic year, local times and week number
			val start = toDateTime(event.getStartDate)
			val end = toDateTime(event.getEndDate) match {
				case null => start
				case date => date
			}

			val day = DayOfWeek(start.getDayOfWeek)

			val year = AcademicYear.findAcademicYearContainingDate(start)

			// Convert the date to an academic week number
			val startWeek = termService.getAcademicWeekForAcademicYear(start, year)

			// We support exactly one RRule of frequence weekly
			val endWeek = event.getProperties(Property.RRULE).asScala.headOption.collect {
				case rule: RRule if rule.getRecur.getFrequency == "WEEKLY" => rule
			}.map { rule =>
				startWeek + rule.getRecur.getCount - 1
			}.getOrElse(startWeek)

			val weekRange = WeekRange(startWeek, endWeek)

			// From the Celcat iCal files we get SUMMARY in the format of
			// (ROOM) - STAFF1/STAFF2 (WEEK_START-WEEK_END)/STAFF3 etc - all optional. Do nasty format parsing
			val staffIds: Seq[UniversityId] =
				if (allStaff.nonEmpty)
					summary.maybeText
						.filter { _.contains(" - ") } // Quick exit - only look for summaries in the right format
						.map { _.split(" - ", 2).last } // Strip the "room" from the start
						.map {
							// Split on / to get a list of names and their optional week numbers
							_.split('/').toSeq
								// Strip the week numbers off the end
								.collect { case r"([^/]+?)${nameOrInitial}(?: (?:[0-9\\-]+,?)+)?" => nameOrInitial }
						}
						.map { namesOrInitials =>
							// Match up against the staff BSV fetched separately
							namesOrInitials.flatMap { nameOrInitial => allStaff.values.find { info =>
								info.fullName == nameOrInitial || info.initials == nameOrInitial
							}.map { _.universityId }}
						}.getOrElse(Nil)
				else Nil

			val staff = userLookup.getUsersByWarwickUniIds(staffIds).values.collect { case FoundUser(u) => u }.toSeq

			Some(TimetableEvent(
				uid = event.getUid.getValue,
				name = summary,
				"",
				description = Option(event.getDescription).map { _.getValue }.filter { _.hasText }.getOrElse(summary),
				eventType = eventType,
				weekRanges = Seq(weekRange),
				day = day,
				startTime = start.toLocalTime,
				endTime = end.toLocalTime,
				location = Option(event.getLocation).flatMap { _.getValue.maybeText }.map(locationFetchingService.locationFor),
				comments = None,
				parent = TimetableEvent.Parent(parseModuleCode(event).flatMap(code => moduleMap.get(code.toLowerCase))),
				staff = staff,
				students = Nil,
				year = year,
				relatedUrl = None,
				attendance = Map()
			))
		}
	}

	// Doesn't support all-day events
	def toDateTime(property: DateProperty): DateTime =
		if (property == null) null
		else new DateTime(property.getDate, getTimeZone(property)).withZoneRetainFields(DateTimeZone.forID("Europe/London"))

	def getTimeZone(property: DateProperty): DateTimeZone =
		if (property.getParameter(Parameter.VALUE) != null && (property.getParameter(Parameter.VALUE) == Value.DATE)) DateTimeZone.UTC
		else if (property.getTimeZone != null) DateTimeZone.forTimeZone(property.getTimeZone)
		else DateTimeZone.forID("Europe/London")

}

class CelcatHttpTimetableFetchingService(celcatConfiguration: CelcatConfiguration) extends StaffTimetableFetchingService with StudentTimetableFetchingService with Logging {
	self: UserLookupComponent
		with TermServiceComponent
		with LocationFetchingServiceComponent
		with CacheStrategyComponent
		with ModuleAndDepartmentServiceComponent
		with DispatchHttpClientComponent =>

	lazy val wbsConfig: CelcatDepartmentConfiguration = celcatConfiguration.wbsConfiguration

	// a dispatch response handler which reads JSON from the response and parses it into a list of TimetableEvents
	def handler(config: CelcatDepartmentConfiguration): (Map[String, Seq[String]], Request) => Handler[EventList] = { (_: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		req >- { (rawJSON) =>
			combineIdenticalEvents(parseJSON(rawJSON))
		}
	}

	def getTimetableForStudent(universityId: UniversityId): Future[EventList] = {
		if (wbsConfig.enabled) doRequest(universityId, wbsConfig)
		else Future.successful(EventList(Nil, None))
	}

	def getTimetableForStaff(universityId: UniversityId): Future[EventList] = {
		if (wbsConfig.enabled) doRequest(universityId, wbsConfig)
		else Future.successful(EventList(Nil, None))
	}

	def doRequest(filename: String, config: CelcatDepartmentConfiguration): Future[EventList] = {
		val req =
			(url(config.baseUri) / filename <<? Map("forcebasic" -> "true"))
				.as_!(config.credentials.username, config.credentials.password)

		// Execute the request
		// If the status is OK, pass the response to the handler function for turning into TimetableEvents
		// else return an empty list.
		logger.info(s"Requesting timetable data from ${req.to_uri.toString}")
		val result =
			Future(httpClient.when(_==200)(req >:+ handler(config)))
				.recover { case StatusCode(404, _) =>
					// Special case a 404, just return no events
					logger.warn(s"Request for ${req.to_uri.toString} returned a 404")
					EventList.fresh(Nil)
				}

		// Extra logging
		result.onFailure { case e =>
			logger.warn(s"Request for ${req.to_uri.toString} failed: ${e.getMessage}")
		}

		result
	}

	def combineIdenticalEvents(events: EventList): EventList = events.map { events =>
		// If we run an identical event in separate weeks, combine the weeks for them
		val groupedEvents = events.groupBy { event =>
			(event.name, event.title, event.description, event.eventType, event.day, event.startTime, event.endTime,
				event.location, event.parent.shortName, event.staff, event.students, event.year)
		}.values.toSeq

		groupedEvents.map { eventSeq => eventSeq.size match {
			case 1 =>
				eventSeq.head
			case _ =>
				val event = eventSeq.head
				TimetableEvent(
					event.uid,
					event.name,
					event.title,
					event.description,
					event.eventType,
					eventSeq.flatMap {
						_.weekRanges
					},
					event.day,
					event.startTime,
					event.endTime,
					event.location,
					event.parent,
					event.comments,
					event.staff,
					event.students,
					event.year,
					event.relatedUrl,
					event.attendance
				)
		}}.toList
	}

	def parseJSON(incomingJson: String): EventList = {
		JSON.parseFull(incomingJson) match {
			case Some(jsonData: List[Map[String, Any]]@unchecked) =>
				EventList.fresh(jsonData.filterNot { event =>
					// TAB-4754 These lectures are already in Syllabus+ so we don't include them again
					event("contactType") == "L" && event("lectureStreamCount") == 1
				}.flatMap { event =>
					val start = DateFormats.IsoDateTime.parseDateTime(event.getOrElse("start", "").toString)
					val end = DateFormats.IsoDateTime.parseDateTime(event.getOrElse("end", "").toString)
					val year = AcademicYear.findAcademicYearContainingDate(start)
					val moduleCode = event.getOrElse("moduleCode","").toString
					val module = moduleAndDepartmentService.getModuleByCode(moduleCode.toLowerCase.safeSubstring(0, expectedModuleCodeLength))
					val parent = TimetableEvent.Parent(module)
					val room = event.getOrElse("room","").toString
					val location = Option(locationFetchingService.locationFor(room))
					val eventType:TimetableEventType = event("contactType") match {
						case "L" => TimetableEventType.Lecture
						case "S" => TimetableEventType.Seminar
						case _ => TimetableEventType.Other("")
					}
					val uid =
						DigestUtils.md5Hex(
							Seq(
								module,
								start.toString,
								end.toString,
								location.fold("") {_.name},
								parent.shortName.getOrElse("")
							).mkString
						)

					Seq(TimetableEvent(
						uid = uid,
						name = moduleCode,
						title = "",
						description = "",
						eventType = eventType,
						weekRanges =  Seq(WeekRange(termService.getAcademicWeekForAcademicYear(start, year))),
						day = DayOfWeek(start.getDayOfWeek),
						startTime = start.toLocalTime,
						endTime = end.toLocalTime,
						location = location,
						comments = None,
						parent = parent,
						staff = Nil,
						students = Nil,
						year = AcademicYear.guessSITSAcademicYearByDate(start),
						relatedUrl = None,
						attendance = Map()
					))
				})
			case _ => throw new RuntimeException("Could not parse JSON")
		}
	}
}

@SerialVersionUID(5445676324342l) case class CelcatStaffInfo(celcatId: String, universityId: UniversityId, initials: String, fullName: String) extends Serializable