package uk.ac.warwick.tabula.data.model.attendance

import org.joda.time.{LocalDate, DateTime}
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model.{GeneratedId, Assignment, Module, MeetingFormat, StudentRelationshipType, HasSettings}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{AssignmentService, ModuleAndDepartmentService, RelationshipService}
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._
import javax.validation.constraints.NotNull
import javax.persistence.{Entity, JoinColumn, FetchType, ManyToOne, Column}
import org.hibernate.annotations.Type

@Entity
class AttendanceMonitoringPoint extends GeneratedId with HasSettings with PostLoadBehaviour {

	import AttendanceMonitoringPoint._

	@transient
	var relationshipService = Wire[RelationshipService]

	@transient
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

	@transient
	var assignmentService = Wire[AssignmentService]

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_id")
	var scheme: AttendanceMonitoringScheme = _

	@NotNull
	var name: String = _

	@Column(name = "start_week")
	var startWeek: Int = _

	@Column(name = "end_week")
	var endWeek: Int = _

	@NotNull
	@Column(name = "start_date")
	var startDate: LocalDate = _

	@NotNull
	@Column(name = "end_date")
	var endDate: LocalDate = _

	@Column(name="point_type")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.MonitoringPointTypeUserType")
	var pointType: MonitoringPointType = _

	@NotNull
	@Column(name = "created_date")
	var createdDate: DateTime = _

	@NotNull
	@Column(name = "updated_date")
	var updatedDate: DateTime = _

	override def postLoad() {
		ensureSettings
	}

	// Setting for MonitoringPointType.Meeting
	def meetingRelationships = getStringSeqSetting(Settings.MeetingRelationships, Seq())
		.map(relationshipService.getStudentRelationshipTypeById(_).getOrElse(null))
	def meetingRelationships_= (relationships: Seq[StudentRelationshipType]):Unit =
		settings += (Settings.MeetingRelationships -> relationships.map(_.id))
	// Ugh. This sucks. But Spring always wants to use the Seq version if they share a method name, and therefore won't bind
	def meetingRelationshipsSpring_= (relationships: JSet[StudentRelationshipType]):Unit = {
		meetingRelationships = relationships.asScala.toSeq
	}

	def meetingFormats = getStringSeqSetting(Settings.MeetingFormats, Seq()).map(MeetingFormat.fromDescription)
	def meetingFormats_= (formats: Seq[MeetingFormat]) =
		settings += (Settings.MeetingFormats -> formats.map(_.description))
	// See above
	def meetingFormatsSpring_= (formats: JSet[MeetingFormat]) =
		meetingFormats = formats.asScala.toSeq

	def meetingQuantity = getIntSetting(Settings.MeetingQuantity, 1)
	def meetingQuantity_= (quantity: Int) = settings += (Settings.MeetingQuantity -> quantity)

	// Setting for MonitoringPointType.SmallGroup

	def smallGroupEventQuantity = getIntSetting(Settings.SmallGroupEventQuantity, 0)
	def smallGroupEventQuantity_= (quantity: Int): Unit = settings += (Settings.SmallGroupEventQuantity -> quantity)
	def smallGroupEventQuantity_= (quantity: JInteger): Unit = {
		smallGroupEventQuantity = quantity match {
			case q: JInteger => q.intValue
			case _ => 0
		}
	}

	def smallGroupEventModules = getStringSeqSetting(Settings.SmallGroupEventModules, Seq())
		.map(moduleAndDepartmentService.getModuleById(_).getOrElse(null))
	def smallGroupEventModules_= (modules: Seq[Module]) =
		settings += (Settings.SmallGroupEventModules -> modules.map(_.id))
	// See above
	def smallGroupEventModulesSpring_= (modules: JSet[Module]) =
		smallGroupEventModules = modules.asScala.toSeq

	// Setting for MonitoringPointType.AssignmentSubmission

	def assignmentSubmissionIsSpecificAssignments = getBooleanSetting(Settings.AssignmentSubmissionIsSpecificAssignments) getOrElse true
	def assignmentSubmissionIsSpecificAssignments_= (allow: Boolean) = settings += (Settings.AssignmentSubmissionIsSpecificAssignments -> allow)

	def assignmentSubmissionQuantity = getIntSetting(Settings.AssignmentSubmissionQuantity, 0)
	def assignmentSubmissionQuantity_= (quantity: Int): Unit = settings += (Settings.AssignmentSubmissionQuantity -> quantity)
	def assignmentSubmissionQuantity_= (quantity: JInteger): Unit = {
		assignmentSubmissionQuantity = quantity match {
			case q: JInteger => q.intValue
			case _ => 0
		}
	}

	def assignmentSubmissionModules = getStringSeqSetting(Settings.AssignmentSubmissionModules, Seq())
		.map(moduleAndDepartmentService.getModuleById(_).getOrElse(null))
	def assignmentSubmissionModules_= (modules: Seq[Module]) =
		settings += (Settings.AssignmentSubmissionModules -> modules.map(_.id))
	// See above
	def assignmentSubmissionModulesSpring_= (modules: JSet[Module]) =
		assignmentSubmissionModules = modules.asScala.toSeq

	def assignmentSubmissionAssignments = getStringSeqSetting(Settings.AssignmentSubmissionAssignments, Seq())
		.map(assignmentService.getAssignmentById(_).getOrElse(null))
	def assignmentSubmissionAssignments_= (assignments: Seq[Assignment]) =
		settings += (Settings.AssignmentSubmissionAssignments -> assignments.map(_.id))
	// See above
	def assignmentSubmissionAssignmentsSpring_= (assignments: JSet[Assignment]) =
		assignmentSubmissionAssignments = assignments.asScala.toSeq

	def assignmentSubmissionIsDisjunction = getBooleanSetting(Settings.AssignmentSubmissionIsDisjunction) getOrElse false
	def assignmentSubmissionIsDisjunction_= (allow: Boolean) = settings += (Settings.AssignmentSubmissionIsDisjunction -> allow)
}

object AttendanceMonitoringPoint {

	object Settings {
		val MeetingRelationships = "meetingRelationships"
		val MeetingFormats = "meetingFormats"
		val MeetingQuantity = "meetingQuantity"

		val SmallGroupEventQuantity = "smallGroupEventQuantity"
		val SmallGroupEventModules = "smallGroupEventModules"

		val AssignmentSubmissionIsSpecificAssignments = "assignmentSubmissionIsSpecificAssignments"
		val AssignmentSubmissionQuantity = "assignmentSubmissionQuantity"
		val AssignmentSubmissionModules = "assignmentSubmissionModules"
		val AssignmentSubmissionAssignments = "assignmentSubmissionAssignments"
		val AssignmentSubmissionIsDisjunction = "assignmentSubmissionIsDisjunction"
	}
}