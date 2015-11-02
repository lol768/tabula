package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import org.springframework.validation.{Errors, BindingResult}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.feedback.OnlineFeedbackState
import uk.ac.warwick.tabula.data.model.MarkingState.MarkingCompleted
import uk.ac.warwick.tabula.data.model._
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User


object BulkModerationApprovalCommand {
	def apply(assignment: Assignment, marker: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new BulkModerationApprovalCommandInternal(assignment, marker, submitter, gradeGenerator)
			with ComposableCommand[Unit]
			with BulkModerationApprovalPermissions
			with BulkModerationApprovalDescription
			with AutowiringUserLookupComponent
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
			with FinaliseFeedbackComponentImpl
}

class BulkModerationApprovalCommandInternal(val assignment: Assignment, val marker: User, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[Unit] with SelfValidating with BulkModerationApprovalState with BindListener with UserAware
	with CanProxy {

	self: StateServiceComponent with FeedbackServiceComponent with FinaliseFeedbackComponent =>

	val user = marker

	override def onBind(result: BindingResult) {
		// filter out any feedbacks where the current user is not the marker
		markerFeedback = markerFeedback.filter(_.getMarkerUser == marker)
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (markerFeedback.isEmpty) errors.rejectValue("students", "markerFeedback.finishMarking.noStudents")
	}

	def applyInternal() {
		markerFeedback.foreach(mf => {
			val parentFeedback = mf.feedback

			// copy the first markers comments to the parent feedback and save
			val firstMarkerFeedback = parentFeedback.retrieveFirstMarkerFeedback
			finaliseFeedback(assignment, Seq(firstMarkerFeedback))
			parentFeedback.updatedDate = DateTime.now
			feedbackService.saveOrUpdate(parentFeedback)

			// mark the moderators marker feedback as completed and save
			stateService.updateState(mf, MarkingCompleted)
		})
	}

}

trait BulkModerationApprovalPermissions extends RequiresPermissionsChecking {
	self: BulkModerationApprovalState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait BulkModerationApprovalDescription extends Describable[Unit] {

	self: BulkModerationApprovalState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> markerFeedback.map(_.feedback.universityId))
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> markerFeedback.size())
	}
}

trait BulkModerationApprovalState extends OnlineFeedbackState {

	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module = assignment.module
	val marker: User
	val submitter: CurrentUser
	val gradeGenerator: GeneratesGradesFromMarks

	var markerFeedback: JList[MarkerFeedback] = JArrayList()
	var confirm: Boolean = false
}