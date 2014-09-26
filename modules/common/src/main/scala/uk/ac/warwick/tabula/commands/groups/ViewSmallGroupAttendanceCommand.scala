package uk.ac.warwick.tabula.commands.groups

import org.joda.time.DateTime
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, MemberOrUser, ReadOnly, TaskBenchmarking, Unaudited}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupEventAttendanceNote, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, AutowiringTermServiceComponent, AutowiringUserLookupComponent, SmallGroupServiceComponent, TermServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap

sealed abstract class SmallGroupAttendanceState {
	def getName = toString
}

object SmallGroupAttendanceState {
	case object Attended extends SmallGroupAttendanceState
	case object MissedAuthorised extends SmallGroupAttendanceState
	case object MissedUnauthorised extends SmallGroupAttendanceState
	case object NotRecorded extends SmallGroupAttendanceState
	case object Late extends SmallGroupAttendanceState
	
	def from(state: Option[AttendanceState]) = state match {
		case Some(AttendanceState.Attended) => Attended
		case Some(AttendanceState.MissedAuthorised) => MissedAuthorised
		case Some(AttendanceState.MissedUnauthorised) => MissedUnauthorised
		case _ => NotRecorded // null
	}
}

object ViewSmallGroupAttendanceCommand {
	type EventInstance = (SmallGroupEvent, SmallGroupEventOccurrence.WeekNumber)
	type PerUserAttendance = SortedMap[User, SortedMap[EventInstance, SmallGroupAttendanceState]]
	type PerUserAttendanceNotes = Map[User, Map[EventInstance, SmallGroupEventAttendanceNote]]
	
	case class SmallGroupAttendanceInformation(
		instances: Seq[EventInstance],
		attendance: PerUserAttendance,
		notes: PerUserAttendanceNotes
	)
	
	def apply(group: SmallGroup) =
		new ViewSmallGroupAttendanceCommand(group)
			with ComposableCommand[SmallGroupAttendanceInformation]
			with ViewSmallGroupAttendancePermissions
			with AutowiringSmallGroupServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringUserLookupComponent
			with ReadOnly with Unaudited {
		override lazy val eventName = "ViewSmallGroupAttendance"
	}
		
	// Sort users by last name, first name
	implicit val defaultOrderingForUser = Ordering.by { user: User => (user.getLastName, user.getFirstName, user.getUserId) }
	
	implicit val defaultOrderingForEventInstance = Ordering.by { instance: EventInstance => instance match {
		case (event, week) => 
			val weekValue = week * 7 * 24
			val dayValue = (event.day.getAsInt - 1) * 24
			val hourValue = event.startTime.getHourOfDay
			
			(weekValue + dayValue + hourValue, week, event.id)
	}}
	
	def allEventInstances(group: SmallGroup, occurrences: Seq[SmallGroupEventOccurrence]) =
		group.events.filter { !_.isUnscheduled }.flatMap { event =>
			val allWeeks = event.weekRanges.flatMap { _.toWeeks }
			allWeeks.map { week => 
				val occurrence = occurrences.find { o =>
					o.event == event && o.week == week
				}
				
				((event, week), occurrence)
			}
		}
		
	def attendanceForStudent(allEventInstances: Seq[(EventInstance, Option[SmallGroupEventOccurrence])], isLate: EventInstance => Boolean)(user: User) = {
		val userAttendance = allEventInstances.map { case ((event, week), occurrence) =>
			val instance = (event, week)
			val attendance = 
				SmallGroupAttendanceState.from(
					occurrence.flatMap { 
						_.attendance.asScala.find { _.universityId == user.getWarwickId }
					}.flatMap { a => Option(a.state) }
				)
			
			val state = 
				if (attendance == SmallGroupAttendanceState.NotRecorded && isLate(event, week)) SmallGroupAttendanceState.Late
				else attendance
			
			instance -> state
		} 
		
		SortedMap(userAttendance.toSeq:_*)
	}
}

class ViewSmallGroupAttendanceCommand(val group: SmallGroup) 
	extends CommandInternal[ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation] with ViewSmallGroupAttendanceState with TaskBenchmarking {
	self: SmallGroupServiceComponent with TermServiceComponent with UserLookupComponent =>
		
	import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand._
	
	if (!group.groupSet.collectAttendance) throw new ItemNotFoundException
	
	override def applyInternal() = {
		val occurrences = benchmarkTask("Get all small group event occurrences for the group") { smallGroupService.findAttendanceByGroup(group) }
			
		// Build a list of all the events and week information, with an optional register
		val instances = benchmarkTask("Translate small group events into instances") { allEventInstances(group, occurrences) }
		
		// Build the list of all users who are in the group, or have attended one or more occurrences of the group
		val allStudents = benchmarkTask("Get a list of all registered or attended users") {
			(group.students.users ++
				userLookup.getUsersByWarwickUniIds(occurrences.flatMap { _.attendance.asScala }.map { _.universityId }).values.toSeq)
			.distinct
		}
		
		val attendance = benchmarkTask("For each student, build an attended list for each instance") { 
			val attendance = allStudents.map { user => user -> attendanceForStudent(instances, isLate)(user) }
			
			SortedMap(attendance.toSeq:_*)
		}

		val existingAttendanceNotes = benchmarkTask("Get attendance notes") {
			smallGroupService.findAttendanceNotes(allStudents.map(_.getWarwickId), occurrences).groupBy(_.student).map{
				case (student, notes) =>
					MemberOrUser(student).asUser -> notes.groupBy(n => (n.occurrence.event, n.occurrence.week)).mapValues(_.head)
			}.toMap.withDefaultValue(Map())
		}
		val attendanceNotes = allStudents.map{ student => student -> existingAttendanceNotes.get(student).getOrElse(Map())}.toMap
		
		SmallGroupAttendanceInformation(
			instances = instances.map { case ((event, week), occurrence) => (event, week) }.sorted,
			attendance = attendance,
			attendanceNotes
		)
	}
	
	lazy val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, group.groupSet.academicYear)
	
	private def isLate(instance: EventInstance): Boolean = instance match {
		case (_, week: SmallGroupEventOccurrence.WeekNumber) =>
			week < currentAcademicWeek // only late if week is in the past
	}
	
}

trait ViewSmallGroupAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewSmallGroupAttendanceState =>
	
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.ViewRegister, group)
	}
}

trait ViewSmallGroupAttendanceState {
	def group: SmallGroup
}