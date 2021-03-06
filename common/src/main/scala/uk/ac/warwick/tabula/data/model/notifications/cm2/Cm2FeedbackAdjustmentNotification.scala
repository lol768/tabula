package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Info
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

object Cm2FeedbackAdjustmentNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/feedback_adjustment_notification.ftl"
}

@Entity
@DiscriminatorValue("Cm2FeedbackAdjustment")
class Cm2FeedbackAdjustmentNotification
	extends NotificationWithTarget[AssignmentFeedback, Assignment]
	with SingleItemNotification[AssignmentFeedback]
	with AutowiringUserLookupComponent
	with MyWarwickActivity {

	def verb = "adjusted"
	def assignment: Assignment = target.entity
	def feedback: Feedback = item.entity

	def recipients: Seq[User] = {
		if (assignment.hasWorkflow) {

			val userId = assignment.markingWorkflow.getStudentsPrimaryMarker(assignment, feedback.usercode).getOrElse({
				throw new IllegalStateException(s"No primary marker found for ${feedback.usercode}")
			})
			Seq(userLookup.getUserByUserId(userId))
		} else {
			Seq()
		}

	}

	def title = s"${assignment.module.code.toUpperCase} - for ${assignment.name} : Adjustments have been made to feedback for ${feedback.studentIdentifier}"

	def content = FreemarkerModel(Cm2FeedbackAdjustmentNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"feedback" -> feedback
	))

	def url: String = recipients.headOption.map(recipient => Routes.admin.assignment.markerFeedback(assignment, recipient)).getOrElse("")
	def urlTitle = "mark these submissions"

	priority = Info

}
