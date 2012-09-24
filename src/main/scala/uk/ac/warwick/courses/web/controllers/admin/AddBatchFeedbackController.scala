package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.assignments.AddFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Participate
import javax.validation.Valid
import uk.ac.warwick.courses.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/batch"))
class AddBatchFeedbackController extends BaseController {
	@ModelAttribute def command(@PathVariable assignment: Assignment, user: CurrentUser) =
		new AddFeedbackCommand(assignment, user)

	def onBind(cmd: AddFeedbackCommand) = cmd.onBind

	// Add the common breadcrumbs to the model.
	def crumbed(mav: Mav, module: Module) = mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	@RequestMapping(method = Array(HEAD, GET))
	def uploadZipForm(@PathVariable module: Module, @PathVariable assignment: Assignment,
		@ModelAttribute cmd: AddFeedbackCommand): Mav = {
		doChecks(module, assignment)
		crumbed(Mav("admin/assignments/feedback/zipform"), module)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@PathVariable module: Module, @PathVariable assignment: Assignment,
		@ModelAttribute cmd: AddFeedbackCommand, errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadZipForm(module, assignment, cmd)
		} else {
			cmd.onBind
			cmd.postExtractValidation(errors)
			doChecks(module, assignment)
			crumbed(Mav("admin/assignments/feedback/zipreview"), module)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable module: Module, @PathVariable assignment: Assignment,
		@ModelAttribute cmd: AddFeedbackCommand, errors: Errors): Mav = {
		doChecks(module, assignment)
		cmd.preExtractValidation(errors)
		cmd.onBind
		cmd.postExtractValidation(errors)
		if (errors.hasErrors) {
			crumbed(Mav("admin/assignments/feedback/zipreview"), module)
		} else {
			// do apply, redirect back
			cmd.apply()
			Redirect(Routes.admin.module(module))
		}
	}

	def doChecks(module: Module, assignment: Assignment) = {
		mustBeLinked(mandatory(assignment), mandatory(module))
		mustBeAbleTo(Participate(module))
	}
}