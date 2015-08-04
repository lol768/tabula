package uk.ac.warwick.tabula.exams.web.controllers.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.commands.assignments.{AdminAddMarksCommand, PostExtractValidation}
import uk.ac.warwick.tabula.coursework.commands.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.data.model.{MarkingWorkflow, Exam, Feedback, Module}
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController
import uk.ac.warwick.tabula.services.{UserLookupService, AssessmentMembershipService, FeedbackService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/{academicYear}/exams/{exam}/marks"))
class ExamsAddMarksController extends ExamsController {

	@Autowired var feedbackService: FeedbackService = _
	@Autowired var examMembershipService: AssessmentMembershipService = _
	@Autowired var userLookup: UserLookupService = _

	type AdminAddMarksCommand = Appliable[Seq[Feedback]] with PostExtractValidation

	@ModelAttribute("adminAddMarksCommand")
	def command(@PathVariable module: Module, @PathVariable exam: Exam, user: CurrentUser): AdminAddMarksCommand =
		AdminAddMarksCommand(mandatory(module), mandatory(exam), user, GenerateGradesFromMarkCommand(mandatory(module), mandatory(exam)))

	// Add the common breadcrumbs to the model.
	def crumbed(mav: Mav, module: Module, academicYear: AcademicYear) = mav.crumbs(
		Breadcrumbs.Department(module.adminDepartment, academicYear),
		Breadcrumbs.Module(module, academicYear)
	)

	@RequestMapping(method = Array(HEAD, GET))
	def viewMarkUploadForm(
		@PathVariable module: Module,
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear,
		@ModelAttribute("adminAddMarksCommand") cmd: AdminAddMarksCommand, errors: Errors
	) = {
		val members = examMembershipService.determineMembershipUsersWithOrder(exam)

		val marksToDisplay = members.map { memberPair =>
			val member = memberPair._1
			val feedback = feedbackService.getStudentFeedback(exam, member.getWarwickId)
			noteMarkItem(member, feedback)
		}

		val studentMarkerMap = members.map(m =>
			MarkingWorkflow.getMarkerFromAssessmentMap(userLookup, m._1.getWarwickId, exam.firstMarkerMap) match {
				case Some(markerId) => m._1.getWarwickId -> userLookup.getUserByUserId(markerId).getFullName
				case None => m._1.getWarwickId -> None
			}).toMap

		crumbed(Mav("exams/admin/marks/marksform",
			"marksToDisplay" -> marksToDisplay,
			"seatNumberMap" -> members.map(m => m._1.getWarwickId -> m._2).toMap,
			"studentMarkerMap" -> studentMarkerMap,
			"isGradeValidation" -> module.adminDepartment.assignmentGradeValidation
		), module, academicYear)

	}

	private def noteMarkItem(member: User, feedback: Option[Feedback]) = {

		logger.debug("in noteMarkItem (logger.debug)")

		val markItem = new MarkItem()
		markItem.universityId = member.getWarwickId

		feedback match {
			case Some(f) =>
				markItem.actualMark = f.actualMark.map { _.toString }.getOrElse("")
				markItem.actualGrade = f.actualGrade.map { _.toString }.getOrElse("")
			case None =>
				markItem.actualMark = ""
				markItem.actualGrade = ""
		}

		markItem
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(
		@PathVariable module: Module,
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear,
		@ModelAttribute("adminAddMarksCommand") cmd: AdminAddMarksCommand, errors: Errors
	) = {
		if (errors.hasErrors) viewMarkUploadForm(module, exam, academicYear, cmd, errors)
		else {
			bindAndValidate(module, cmd, errors)
			crumbed(Mav("exams/admin/marks/markspreview"), module, academicYear)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(
		@PathVariable module: Module,
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear,
		@ModelAttribute("adminAddMarksCommand") cmd: AdminAddMarksCommand, errors: Errors
	) = {
		bindAndValidate(module, cmd, errors)
		cmd.apply()
		Redirect(Routes.admin.module(module, academicYear))
	}

	private def bindAndValidate(module: Module, cmd: AdminAddMarksCommand, errors: Errors) {
		cmd.postExtractValidation(errors)
	}

}