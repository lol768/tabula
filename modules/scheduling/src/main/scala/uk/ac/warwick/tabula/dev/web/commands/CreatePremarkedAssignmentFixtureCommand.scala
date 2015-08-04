package uk.ac.warwick.tabula.dev.web.commands

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.model.MarkingState.MarkingCompleted
import uk.ac.warwick.tabula.data.{UserGroupDao, TransactionalComponent, AutowiringTransactionalComponent}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import scala.collection.JavaConverters._

class CreatePremarkedAssignmentFixtureCommand extends CommandInternal[Assignment] with Logging {

	this: TransactionalComponent =>

	val assignmentSrv = Wire[AssessmentService]
	val userLookup = Wire[UserLookupService]
	val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
	val markingWorkflowService = Wire[MarkingWorkflowService]
	val submissionService = Wire[SubmissionService]
	val feedbackService = Wire[FeedbackService]
	val userGroupDao = Wire[UserGroupDao]

	var moduleCode: String = _

	protected def applyInternal() = {
		val module = moduleAndDepartmentService.getModuleByCode(moduleCode).getOrElse(
			throw new IllegalArgumentException(s"wrong module code $moduleCode")
		)

		val assignment = new Assignment
		assignment.module = module
		assignment.name = "Premarked assignment"
		assignment.setDefaultBooleanProperties()
		assignment.addDefaultFields()
		assignment.openDate = DateTime.now.minusDays(30)
		assignment.closeDate = DateTime.now
		assignment.openEnded = false
		assignment.collectMarks = true
		assignment.collectSubmissions = true
		assignment.restrictSubmissions = true
		assignment.allowLateSubmissions = true
		assignment.allowResubmission = false
		assignment.displayPlagiarismNotice = true
		assignment.allowExtensions = true
		assignment.summative = false

		transactional() {

			// persist the assignment to give it an ID
			assignmentSrv.save(assignment)

			val workflow = new FirstMarkerOnlyWorkflow(module.adminDepartment)
			workflow.name = "First marker only workflow"
			workflow.firstMarkers.knownType.includedUserIds = CreatePremarkedAssignmentFixtureCommand.firstMarkers
			markingWorkflowService.save(workflow)
			assignment.markingWorkflow = workflow

			val group = UserGroup.ofUniversityIds
			assignment.members = group
			assignment.members.knownType.addUserId("3000001")
			assignment.members.knownType.addUserId("3000003")
			userGroupDao.saveOrUpdate(group)


			val marker1Group = UserGroup.ofUniversityIds
			marker1Group.addUserId("3000001")
			marker1Group.addUserId("3000003")
			userGroupDao.saveOrUpdate(marker1Group)
			val map1 = FirstMarkersMap(assignment, "tabula-functest-marker1", marker1Group)
			assignment.firstMarkers = Seq(map1).asJava

			val submissions = CreatePremarkedAssignmentFixtureCommand.students.map(student => {
				val s = new Submission
				s.assignment = assignment
				s.userId = student.userId
				s.universityId = student.universityId
				s.submittedDate = DateTime.now.minusDays(30)
				s
			})
			submissions.foreach(submissionService.saveSubmission)
			assignment.submissions = submissions.asJava

			val feedbacks = CreatePremarkedAssignmentFixtureCommand.students.map(student => {
				val f = new AssignmentFeedback
				f.universityId = student.universityId
				f.assignment = assignment
				f.uploaderId = "tabula-functest-admin1"
				val mf = f.retrieveFirstMarkerFeedback
				mf.mark = Some(41)
				mf.state = MarkingCompleted
				f.actualMark = Some(41)
				f
			})
			val markerFeedbacks = feedbacks.map(_.firstMarkerFeedback)
			feedbacks.foreach(feedbackService.saveOrUpdate)
			markerFeedbacks.foreach(feedbackService.save)
			assignment.feedbacks = feedbacks.asJava
			assignmentSrv.save(assignment)
		}

		assignment
	}
}

object CreatePremarkedAssignmentFixtureCommand {

	case class Student(universityId: String, userId: String)

	val students = Seq(Student("3000001", "tabula-functest-student1"), Student("3000003", "tabula-functest-student3"))
	val firstMarkers = Seq("tabula-functest-marker1")

	def apply() ={
		new CreatePremarkedAssignmentFixtureCommand
			with ComposableCommand[Assignment]
			with AutowiringTransactionalComponent
			with PubliclyVisiblePermissions
			with Unaudited
	}
}