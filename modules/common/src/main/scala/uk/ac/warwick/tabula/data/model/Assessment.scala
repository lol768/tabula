package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentMembershipInfo}
import collection.JavaConverters._

trait Assessment extends GeneratedId with CanBeDeleted with PermissionsTarget {

	@transient
	var assessmentMembershipService = Wire[AssessmentMembershipService]("assignmentMembershipService")
	var module: Module
	var academicYear: AcademicYear
	var name: String
	var assessmentGroups: JList[AssessmentGroup]
	var markingWorkflow: MarkingWorkflow
	var firstMarkers: JList[FirstMarkersMap]
	var secondMarkers: JList[SecondMarkersMap]

	/** Map between first markers and the students assigned to them */
	def firstMarkerMap: Map[String, UserGroup] = Option(firstMarkers).map { markers => markers.asScala.map {
		markerMap => markerMap.marker_id -> markerMap.students
	}.toMap }.getOrElse(Map())

	/** Map between second markers and the students assigned to them */
	def secondMarkerMap: Map[String, UserGroup] = Option(secondMarkers).map { markers => markers.asScala.map {
		markerMap => markerMap.marker_id -> markerMap.students
	}.toMap }.getOrElse(Map())


	def addDefaultFeedbackFields(): Unit
	def addDefaultFields(): Unit
	def allFeedback: Seq[Feedback]

	// feedback that has been been through the marking process (not placeholders for marker feedback)
	def fullFeedback = allFeedback.filterNot(_.isPlaceholder).toSeq

	// returns feedback for a specified student
	def findFeedback(uniId: String) = allFeedback.find(_.universityId == uniId)

	// returns feedback for a specified student
	def findFullFeedback(uniId: String) = fullFeedback.find(_.universityId == uniId)

	// converts the assessmentGroups to upstream assessment groups
	def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] =
		assessmentGroups.asScala.flatMap {
			_.toUpstreamAssessmentGroup(academicYear)
		}

	// Gets a breakdown of the membership for this assessment. Note that this cannot be sorted by seat number
	def membershipInfo: AssessmentMembershipInfo = assessmentMembershipService.determineMembership(this)

	def collectMarks: JBoolean

	def hasWorkflow: Boolean

	def isReleasedForMarking(universityId: String): Boolean =
		allFeedback.find(_.universityId == universityId) match {
			case Some(f) => f.firstMarkerFeedback != null
			case _ => false
		}

	def isReleasedToSecondMarker(universityId: String): Boolean =
		allFeedback.find(_.universityId == universityId) match {
			case Some(f) => f.secondMarkerFeedback != null
			case _ => false
		}

	def isReleasedToThirdMarker(universityId: String): Boolean =
		allFeedback.find(_.universityId == universityId) match {
			case Some(f) => f.thirdMarkerFeedback != null
			case _ => false
		}

}
