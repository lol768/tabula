package uk.ac.warwick.tabula.data.model.notifications.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MeetingRecord, NotificationWithTarget, SingleItemNotification, StudentRelationship}

abstract class MeetingRecordApprovalNotification(val verb: String)
	extends NotificationWithTarget[MeetingRecord, StudentRelationship]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecord] {

	override def onPreSave(newRecord: Boolean) {
		// if the meeting took place more than a week ago then this is more important
		priority = if (meeting.meetingDate.isBefore(DateTime.now.minusWeeks(1))) {
			Critical
		} else {
			Warning
		}
	}

	def meeting = item.entity
	def relationship = target.entity

	def title = s"${agentRole.capitalize} meeting needs review"
	def actionRequired = true
	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> meeting.creator.asSsoUser,
		"role"-> agentRole,
		"dateFormatter" -> dateOnlyFormatter,
		"verbed" ->  (if (verb == "create") "created" else "edited"),
		"meetingRecord" -> meeting
	))
	def recipients = meeting.pendingApprovers.map(_.asSsoUser)
	def urlTitle = "review the meeting record"

}

@Entity
@DiscriminatorValue("newMeetingRecordApproval")
class NewMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("create")

@Entity
@DiscriminatorValue("editedMeetingRecordApproval")
class EditedMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("edit")