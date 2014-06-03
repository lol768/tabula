package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.commands.assignments.{ViewOnlineFeedbackCommand, SubmitAssignmentCommand}
import uk.ac.warwick.tabula.data.model.{Submission, Assignment, Module}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.MonitoringPointProfileTermAssignmentService
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.commands.{StudentSubmissionAndFeedbackCommand, CurrentUserSubmissionAndFeedbackCommandState}
import uk.ac.warwick.tabula.coursework.commands.StudentSubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.commands.Appliable

/**
 * This is the main student-facing and non-student-facing controller for handling esubmission and return of feedback.
 * If the studentMember is not specified it works for the current user, whether they are a member of not.
 */
@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}")
class AssignmentController extends CourseworkController {

	type StudentSubmissionAndFeedbackCommand = Appliable[StudentSubmissionInformation] with CurrentUserSubmissionAndFeedbackCommandState

	var monitoringPointProfileTermAssignmentService = Wire[MonitoringPointProfileTermAssignmentService]

	hideDeletedItems

	validatesSelf[SubmitAssignmentCommand]

	@ModelAttribute("submitAssignmentCommand") def formOrNull(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) = {
		val cmd = new ViewOnlineFeedbackCommand(assignment, user)
		
		val feedback =
			if (cmd.hasFeedback) cmd.apply() // Log audit event
			else None

		restrictedBy {
			feedback.isDefined
		} (new SubmitAssignmentCommand(mandatory(module), mandatory(assignment), user)).orNull
	}

	@ModelAttribute("studentSubmissionAndFeedbackCommand") def studentSubmissionAndFeedbackCommand(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) =
		StudentSubmissionAndFeedbackCommand(module, assignment, user)

	@ModelAttribute("willCheckpointBeCreated") def willCheckpointBeCreated(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		user: CurrentUser) = {
			val submission = new Submission(user.universityId)
			submission.assignment = assignment
			submission.submittedDate = DateTime.now
			submission.userId = user.userId
			!monitoringPointProfileTermAssignmentService.getCheckpointsForSubmission(submission).isEmpty
	}

	/**
	 * Sitebuilder-embeddable view.
	 */
	@RequestMapping(method = Array(HEAD, GET), params = Array("embedded"))
	def embeddedView(
			@ModelAttribute("studentSubmissionAndFeedbackCommand") infoCommand: StudentSubmissionAndFeedbackCommand,
			@ModelAttribute("submitAssignmentCommand") formOrNull: SubmitAssignmentCommand,
			errors: Errors) = {
		view(infoCommand, formOrNull, errors).embedded
	}

	@RequestMapping(method = Array(HEAD, GET), params = Array("!embedded"))
	def view(
			@ModelAttribute("studentSubmissionAndFeedbackCommand") infoCommand: StudentSubmissionAndFeedbackCommand,
			@ModelAttribute("submitAssignmentCommand") formOrNull: SubmitAssignmentCommand,
			errors: Errors) = {
		val form = Option(formOrNull)
		val info = infoCommand.apply()

		// If the user has feedback but doesn't have permission to submit, form will be null here, so we can't just get module/assignment from that
		Mav(
			"submit/assignment",
			"feedback" -> info.feedback,
			"submission" -> info.submission,
			"justSubmitted" -> form.exists { _.justSubmitted },
			"canSubmit" -> info.canSubmit,
			"canReSubmit" -> info.canReSubmit,
			"hasExtension" -> info.extension.isDefined,
			"hasActiveExtension" -> info.extension.exists(_.approved), // active = has been approved
			"extension" -> info.extension,
			"isExtended" -> info.isExtended,
			"extensionRequested" -> info.extensionRequested)
			.withTitle(infoCommand.module.name + " (" + infoCommand.module.code.toUpperCase + ")" + " - " + infoCommand.assignment.name)
	}

	@RequestMapping(method = Array(POST))
	def submit(
			@ModelAttribute("studentSubmissionAndFeedbackCommand") infoCommand: StudentSubmissionAndFeedbackCommand,
			@Valid @ModelAttribute("submitAssignmentCommand") form: SubmitAssignmentCommand,
			errors: Errors) = {
		// We know form isn't null here because of permissions checks on the info command
		if (errors.hasErrors) {
			view(infoCommand, form, errors)
		} else {
			transactional() { form.apply() }

			Redirect(Routes.assignment(form.assignment)).addObjects("justSubmitted" -> true)
		}
	}

}
