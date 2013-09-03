package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, Appliable, CommandInternal}
import uk.ac.warwick.tabula.profiles.services.timetables._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking, PubliclyVisiblePermissions}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.services._
import org.joda.time.{Interval, LocalDate}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.helpers.DateTimeOrdering

trait ViewStudentPersonalTimetableCommandState {
	var student: StudentMember = _
	var start: LocalDate = LocalDate.now.minusMonths(12)
	var end: LocalDate = start.plusMonths(13)
}

class ViewStudentPersonalTimetableCommandImpl(studentTimetableEventSource:StudentTimetableEventSource) extends CommandInternal[Seq[EventOccurrence]] with ViewStudentPersonalTimetableCommandState {
	this: EventOccurrenceServiceComponent =>

	def eventsToOccurrences: TimetableEvent => Seq[EventOccurrence] =
		eventOccurrenceService.fromTimetableEvent(_, new Interval(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay))

	def applyInternal(): Seq[EventOccurrence] = {
		val timetableEvents = studentTimetableEventSource.eventsFor(student)
		val occurrences = timetableEvents flatMap eventsToOccurrences
		// Converter to make localDates sortable
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering.orderedLocalDateTime
		occurrences.sortBy(_.start)
	}
}

trait ViewStudentTimetablePermissions extends RequiresPermissionsChecking{
	this:ViewStudentPersonalTimetableCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.Timetable, student)
	}
}

object ViewStudentPersonalTimetableCommand {


	// mmm, cake.
	def apply(eventSource:StudentTimetableEventSource): Appliable[Seq[EventOccurrence]] with ViewStudentPersonalTimetableCommandState = {

		new ViewStudentPersonalTimetableCommandImpl(eventSource)
			with ComposableCommand[Seq[EventOccurrence]]
			with ViewStudentTimetablePermissions
			with Unaudited
			with TermBasedEventOccurrenceComponent
			with TermAwareWeekToDateConverterComponent
			with AutowiringTermFactoryComponent
	}
}


