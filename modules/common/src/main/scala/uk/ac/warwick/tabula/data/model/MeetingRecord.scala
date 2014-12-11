package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence._

import org.hibernate.annotations.BatchSize
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

import scala.collection.JavaConverters._

object MeetingRecord {
	val DefaultMeetingTimeOfDay = 12 // Should be used for legacy meetings (where isRealTime is false)
	val MeetingTooOldThresholdYears = 5
	val MaxTitleLength = 140
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("standard")
class MeetingRecord extends AbstractMeetingRecord {

	def this(creator: Member, relationship: StudentRelationship) {
		this()
		this.creator = creator
		this.relationship = relationship
	}

	@transient
	var securityService = Wire[SecurityService]

	@Column(name="real_time")
	var isRealTime: Boolean = true

	def toEventOccurrence(context: TimetableEvent.Context): Option[EventOccurrence] = {
		if (isRealTime) {
			this.asEventOccurrence(context)
		}	else {
			None
		}
	}

	// Workflow definitions

	@OneToMany(mappedBy="meetingRecord", fetch=FetchType.LAZY, cascade=Array(ALL))
	@BatchSize(size=200)
	var approvals: JList[MeetingRecordApproval] = JArrayList()

	// true if the specified user needs to perform a workflow action on this meeting record
	def pendingActionBy(user: CurrentUser): Boolean = pendingApprovalBy(user) || pendingRevisionBy(user)

	// if there are no approvals with a state of approved return true - otherwise, all approvals need to be true
	def isApproved = !approvals.asScala.exists(approval => !(approval.state == Approved))

	// for attendance purposes the meeting is approved if it was created by the agent, or is otherwise approved
	def isAttendanceApproved = (creator != null && relationship != null && creator.universityId == relationship.agent) || isApproved

	def isPendingApproval = approvals.asScala.exists(approval => approval.state == Pending)
	def pendingApprovals = approvals.asScala.filter(_.state == Pending)
	def pendingApprovalBy(user: CurrentUser): Boolean =
		approvals.asScala.exists(approval =>
			approval.state == Pending &&
				user.universityId != creator.universityId &&
				(pendingApprovals.exists(_.approver.universityId == user.apparentUser.getWarwickId ) ||
					securityService.can(user, Permissions.Profiles.MeetingRecord.Approve, approval))
	)

	def pendingApprovers:List[Member] = pendingApprovals.map(_.approver).toList

	def isRejected =  approvals.asScala.exists(approval => approval.state == Rejected)
	def rejectedApprovals = approvals.asScala.filter(_.state == Rejected)
	def rejectedBy(member: Member): Boolean = rejectedApprovals.exists(_.approver == member)
	// people who have had a draft version rejected
	def pendingRevisionBy(user: CurrentUser) = isRejected && user.universityId == creator.universityId

	// End of workflow definitions
}