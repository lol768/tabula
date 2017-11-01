package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

import scala.collection.JavaConverters._

object ReleaseToMarkerNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/released_to_marker_notification.ftl"
}

@Entity
@DiscriminatorValue("CM2ReleaseToMarker")
class ReleaseToMarkerNotification
	extends NotificationWithTarget[MarkerFeedback, Assignment]
	with SingleRecipientNotification
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent
	with Logging
	with AllCompletedActionRequiredNotification {

	def workflowVerb: String = items.asScala.headOption.map(_.entity.stage.verb).getOrElse(MarkingWorkflowStage.DefaultVerb)

	def verb = "released"
	def assignment: Assignment = target.entity

	def title: String = "%s: Submissions for \"%s\" have been released for marking".format(assignment.module.code.toUpperCase, assignment.name)

	def noSubmissionsCnt = (items.size - items.asScala.find(_ => assignment.submissions.size() > 0).size)

	def content = FreemarkerModel(ReleaseToMarkerNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"numAllocated" -> assignment.cm2MarkerAllocations.filter(_.marker == recipient).flatMap(_.students).distinct.size,
			"numReleasedFeedbacks" -> items.size,
			"numReleasedSubmissionsFeedbacks" -> items.asScala.find(_ => assignment.submissions.size() > 0).size,
			"numReleasedNoSubmissionsFeedbacks" -> noSubmissionsCnt,
			"workflowVerb" -> workflowVerb
		))
	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)
	def urlTitle = s"The assignment '${assignment.module.code.toUpperCase} - ${assignment.name}' is ready for you to mark."

	priority = Warning

}