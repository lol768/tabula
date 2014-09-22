package uk.ac.warwick.tabula.groups.commands.admin

import org.joda.time.LocalTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Location, Module, Department}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.{LazyLists, SystemClockComponent, FoundUser}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.{AutowiringScientiaConfigurationComponent, ScientiaHttpTimetableFetchingService, ModuleTimetableFetchingServiceComponent}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}

import scala.collection.JavaConverters._

import ImportSmallGroupSetsFromExternalSystemCommand._

object ImportSmallGroupSetsFromExternalSystemCommand {
	val RequiredPermission = Permissions.SmallGroups.ImportFromExternalSystem

	case class TimetabledSmallGroupEvent(
		module: Module,
		eventType: TimetableEventType,
		events: Seq[TimetableEvent]
	)

	def apply(department: Department, user: CurrentUser) =
		new ImportSmallGroupSetsFromExternalSystemCommandInternal(department, user)
			with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
			with ImportSmallGroupSetsFromExternalSystemPermissions
			with ImportSmallGroupSetsFromExternalSystemValidation
			with ImportSmallGroupSetsFromExternalSystemDescription
			with SetDefaultSelectedValue
			with AutowiringSecurityServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with ComposableCommand[Seq[SmallGroupSet]]
			with CommandSmallGroupSetGenerator
			with CommandSmallGroupEventGenerator
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringScientiaConfigurationComponent
			with SystemClockComponent
			with ModuleTimetableFetchingServiceComponent {
				val timetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)
			}

	def generateSmallGroupSetNameAndFormat(module: Module, eventType: TimetableEventType) = {
		val format = eventType match {
			case TimetableEventType.Seminar => SmallGroupFormat.Seminar
			case _ => SmallGroupFormat.Workshop
		}
		val name = "%s %ss".format(module.code.toUpperCase, format)

		(name, format)
	}
}

trait ImportSmallGroupSetsFromExternalSystemCommandState extends CurrentSITSAcademicYear {
	def department: Department
	def user: CurrentUser

	var selected: JList[JBoolean] = LazyLists.create({ () => true })
}

trait ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState {
	self: ImportSmallGroupSetsFromExternalSystemCommandState with ModuleAndDepartmentServiceComponent with SecurityServiceComponent =>

	lazy val canManageDepartment = securityService.can(user, RequiredPermission, department)
	lazy val modulesWithPermission = moduleAndDepartmentService.modulesWithPermission(user, RequiredPermission, department)

	lazy val modules =
		if (canManageDepartment) department.modules.asScala
		else modulesWithPermission
}

trait LookupEventsFromModuleTimetables {
	self: ModuleTimetableFetchingServiceComponent with ImportSmallGroupSetsFromExternalSystemCommandState with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState =>

	lazy val timetabledEvents =
		modules.toSeq
			.flatMap { module =>
			val events =
				timetableFetchingService.getTimetableForModule(module.code.toUpperCase)
					.filter { event => event.year == academicYear }
					.filter { event => event.eventType == TimetableEventType.Practical || event.eventType == TimetableEventType.Seminar }
					.groupBy { _.eventType }

			events.toSeq.map { case (eventType, events) => new TimetabledSmallGroupEvent(module, eventType, events.sortBy { event => (event.weekRanges.minBy { _.minWeek }.minWeek, event.day.jodaDayOfWeek, event.startTime.getMillisOfDay) }) }
		}.sortBy { e => (e.module.code, e.eventType.code) }

}

trait SetDefaultSelectedValue extends PopulateOnForm {
	self: ImportSmallGroupSetsFromExternalSystemCommandState with LookupEventsFromModuleTimetables =>

	def populate() = {
		timetabledEvents.zipWithIndex.foreach { case (TimetabledSmallGroupEvent(module, eventType, _), i) =>
			val (name, _) = generateSmallGroupSetNameAndFormat(module, eventType)

			selected.get(i) // Run a get() first to ensure that the list has been grown
			selected.set(i, !module.groupSets.asScala.exists { set =>
				set.academicYear == academicYear &&
				!set.archived && !set.deleted &&
				set.name == name
			})
		}
	}
}

class ImportSmallGroupSetsFromExternalSystemCommandInternal(val department: Department, val user: CurrentUser)
	extends CommandInternal[Seq[SmallGroupSet]]
		with ImportSmallGroupSetsFromExternalSystemCommandState
		with LookupEventsFromModuleTimetables {
	self: ModuleTimetableFetchingServiceComponent
		with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
		with SmallGroupServiceComponent
		with SmallGroupSetGenerator
		with SmallGroupEventGenerator
		with UserLookupComponent =>

	def applyInternal() = transactional() {
		// For each combination of module & event type, create a small group set with a group for each event
		timetabledEvents.zipWithIndex
			.filter { case (_, i) => selected.get(i) }
			.map { case (TimetabledSmallGroupEvent(module, eventType, events), i) =>
				val (name, format) = generateSmallGroupSetNameAndFormat(module, eventType)

				val set = createSet(module, format, name)

				events.zipWithIndex.foreach { case (e, i) =>
					val group = new SmallGroup(set)
					group.name = s"Group ${i + 1}"
					group.students.knownType.includedUserIds = e.studentUniversityIds

					smallGroupService.saveOrUpdate(group)

					set.groups.add(group)

					val tutorUsercodes = e.staffUniversityIds.flatMap { id =>
						Option(userLookup.getUserByWarwickUniId(id)).collect { case FoundUser(u) => u }.map { _.getUserId }
					}

					createEvent(module, set, group, e.weekRanges, e.day, e.startTime, e.endTime, e.location, tutorUsercodes)
				}

				smallGroupService.saveOrUpdate(set)
				set
			}
	}
}

trait SmallGroupSetGenerator {
	def createSet(module: Module, format: SmallGroupFormat, name: String): SmallGroupSet
}

trait CommandSmallGroupSetGenerator extends SmallGroupSetGenerator {
	self: ImportSmallGroupSetsFromExternalSystemCommandState =>

	def createSet(module: Module, format: SmallGroupFormat, name: String) = {
		val command = ModifySmallGroupSetCommand.create(module)
		command.format = format
		command.name = name
		command.academicYear = academicYear

		command.apply()
	}
}

trait SmallGroupEventGenerator {
	def createEvent(module: Module, set: SmallGroupSet, group: SmallGroup, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], tutorUsercodes: Seq[String]): SmallGroupEvent
}

trait CommandSmallGroupEventGenerator extends SmallGroupEventGenerator {
	def createEvent(module: Module, set: SmallGroupSet, group: SmallGroup, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], tutorUsercodes: Seq[String]) = {
		val command = ModifySmallGroupEventCommand.create(module, set, group)
		command.weekRanges = weeks
		command.day = day
		command.startTime = startTime
		command.endTime = endTime
		location.collect {
			case NamedLocation(name) => command.location = name

			case MapLocation(name, locationId) =>
				command.location = name
				command.locationId = locationId
		}

		command.tutors.addAll(tutorUsercodes.asJavaCollection)

		command.apply()
	}
}

trait ImportSmallGroupSetsFromExternalSystemPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ImportSmallGroupSetsFromExternalSystemCommandState with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
		with SecurityServiceComponent =>

	def permissionsCheck(p: PermissionsChecking) {
		if (canManageDepartment) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			p.PermissionCheck(RequiredPermission, department)
		} else {
			val managedModules = modulesWithPermission.toList

			// This is implied by the above, but it's nice to check anyway. Avoid exception if there are no managed modules
			if (!managedModules.isEmpty) p.PermissionCheckAll(RequiredPermission, managedModules)
			else p.PermissionCheck(RequiredPermission, department)
		}
	}
}

trait ImportSmallGroupSetsFromExternalSystemValidation extends SelfValidating {
	self: ImportSmallGroupSetsFromExternalSystemCommandState =>

	def validate(errors: Errors): Unit = {

	}
}

trait ImportSmallGroupSetsFromExternalSystemDescription extends Describable[Seq[SmallGroupSet]] {
	self: ImportSmallGroupSetsFromExternalSystemCommandState =>

	override def describe(d: Description) =
		d.department(department)
}