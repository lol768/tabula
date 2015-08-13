package uk.ac.warwick.tabula.api.commands.coursework.turnitinlti

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.helpers.Logging
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService

object TurnitinLtiSubmitAssignmentResponseCommand {
	def apply(assignment: Assignment) = new TurnitinLtiSubmitAssignmentResponseCommandInternal(assignment)
		with ComposableCommand[Unit]
		with TurnitinLtiSubmitAssignmentResponseCommandDescription
		with PubliclyVisiblePermissions
		with AutowiringAssessmentServiceComponent
		with TurnitinLtiSubmitAssignmentResponseValidation

}

class TurnitinLtiSubmitAssignmentResponseCommandInternal(val assignment: Assignment) extends CommandInternal[Unit]
with TurnitinLtiSubmitAssignmentResponseCommandState with Logging  {
	self: AssessmentServiceComponent =>

	override protected def applyInternal() = {
		assignment.turnitinId = assignmentid
		assessmentService.save(assignment)
	}
}

trait TurnitinLtiSubmitAssignmentResponseRequestState {

	var assignmentid: String = _
	var resource_link_id: String = _
}

trait TurnitinLtiSubmitAssignmentResponseCommandState extends TurnitinLtiSubmitAssignmentResponseRequestState {
	def assignment: Assignment
}

trait TurnitinLtiSubmitAssignmentResponseValidation extends SelfValidating {
	self: TurnitinLtiSubmitAssignmentResponseCommandState with AssessmentServiceComponent =>
	override def validate(errors: Errors) = {
		if (assignment != assessmentService.getAssignmentById(resource_link_id.substring(TurnitinLtiService.AssignmentPrefix.length)).get) {
			errors.rejectValue("assignment", "turnitin.assignment.invalid")
		}
	}

}

trait TurnitinLtiSubmitAssignmentResponseCommandDescription extends Describable[Unit] {
	self: TurnitinLtiSubmitAssignmentResponseCommandState =>

	override lazy val eventName = "TurnitinLtiSubmitAssignmentResponse"

	def describe(d: Description) {
		d.assignment(assignment)
		d.property("existing turnitin id", assignment.turnitinId)
		d.property("new turnitin id", assignmentid)
	}

}