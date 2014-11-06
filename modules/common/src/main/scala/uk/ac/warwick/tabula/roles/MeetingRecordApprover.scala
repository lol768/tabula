package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions.Profiles.MeetingRecord

case class MeetingRecordApprover(meeting: model.MeetingRecord) extends BuiltInRole(MeetingRecordApproverRoleDefinition, meeting)

case object MeetingRecordApproverRoleDefinition extends UnassignableBuiltInRoleDefinition {
	override def description = "Approver"

	GrantsScopedPermission(
		MeetingRecord.Approve
	)
}