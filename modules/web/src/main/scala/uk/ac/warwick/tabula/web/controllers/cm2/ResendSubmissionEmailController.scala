package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.coursework.assignments.SendSubmissionReceiptCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/submission/{assignment}/resend-receipt"))
class ResendSubmissionEmailController extends CourseworkController {

	var submissionService = Wire.auto[SubmissionService]

	hideDeletedItems

	@ModelAttribute def command(@PathVariable assignment: Assignment, user: CurrentUser) =
		new SendSubmissionReceiptCommand(
			assignment.module, assignment,
			mandatory(submissionService.getSubmissionByUniId(assignment, user.universityId).filter(_.submitted)),
			user)

	@RequestMapping(method = Array(GET, HEAD))
	def nope(form: SendSubmissionReceiptCommand) = Redirect(Routes.assignment(mandatory(form.assignment)))

	@RequestMapping(method = Array(POST))
	def sendEmail(form: SendSubmissionReceiptCommand): Mav = {
		val sent = form.apply()

		Mav(s"$urlPrefix/submit/receipt",
			"submission" -> form.submission,
			"module" -> form.module,
			"assignment" -> form.assignment,
			"sent" -> sent,
			"hasEmail" -> user.email.hasText)

	}

}
