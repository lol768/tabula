package uk.ac.warwick.tabula.commands.timetables

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember, Member}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.util.Try

object ViewMemberTimetableCommand extends Logging {
	type TimetableCommand = Appliable[Try[Seq[TimetableEvent]]] with ViewMemberTimetableRequest with SelfValidating

	def apply(member: Member, currentUser: CurrentUser): TimetableCommand = member match {
		case student: StudentMember =>
			new ViewStudentTimetableCommandInternal(student, currentUser)
				with ComposableCommand[Try[Seq[TimetableEvent]]]
				with ViewMemberTimetablePermissions
				with ViewMemberTimetableValidation
				with Unaudited with ReadOnly
				with AutowiringStudentTimetableEventSourceComponent

		case staff: StaffMember =>
			new ViewStaffTimetableCommandInternal(staff, currentUser)
				with ComposableCommand[Try[Seq[TimetableEvent]]]
				with ViewMemberTimetablePermissions
				with ViewMemberTimetableValidation
				with Unaudited with ReadOnly
				with AutowiringStaffTimetableEventSourceComponent

		case _ =>
			logger.error(s"Don't know how to render timetables for non-student or non-staff users (${member.universityId}, ${member.userType})")
			throw new ItemNotFoundException
	}
}

abstract class ViewStudentTimetableCommandInternal(val member: StudentMember, currentUser: CurrentUser)
	extends CommandInternal[Try[Seq[TimetableEvent]]]
		with ViewMemberTimetableRequest {

	self: StudentTimetableEventSourceComponent =>

	def applyInternal(): Try[Seq[TimetableEvent]] = {
		studentTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Student)
			.map { events => events.filter { event => event.year == academicYear }}
	}

}

abstract class ViewStaffTimetableCommandInternal(val member: StaffMember, currentUser: CurrentUser)
	extends CommandInternal[Try[Seq[TimetableEvent]]]
	with ViewMemberTimetableRequest {

	self: StaffTimetableEventSourceComponent =>

	def applyInternal(): Try[Seq[TimetableEvent]] = {
		staffTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Staff)
			.map { events => events.filter { event => event.year == academicYear }}
	}

}

// State - unmodifiable pre-requisites
trait ViewMemberTimetableState {
	val member: Member
}

// Request parameters
trait ViewMemberTimetableRequest extends ViewMemberTimetableState
	with CurrentSITSAcademicYear

trait ViewMemberTimetablePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMemberTimetableState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.Timetable, mandatory(member))
	}
}

trait ViewMemberTimetableValidation extends SelfValidating {
	self: ViewMemberTimetableRequest =>

	override def validate(errors: Errors) {
		if (academicYear == null) {
			errors.rejectValue("academicYear", "NotEmpty")
		}
	}
}