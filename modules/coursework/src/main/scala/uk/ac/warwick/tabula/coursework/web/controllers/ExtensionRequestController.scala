package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.coursework.assignments.extensions._
import uk.ac.warwick.tabula.data.model.forms.Extension
import javax.validation.Valid
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire

@Controller
@RequestMapping(value=Array("/module/{module}/{assignment}/extension"))
class ExtensionRequestController extends CourseworkController{

	var profileService = Wire.auto[ProfileService]

	@ModelAttribute("command")
	def cmd(
		@PathVariable module: Module,
		@PathVariable assignment:Assignment,
		@RequestParam(defaultValue = "")
		action: String,
		user:CurrentUser
	) =
		RequestExtensionCommand(module, assignment, user, action)

	validatesSelf[SelfValidating]

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module)
		= mav.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))

	@RequestMapping(method=Array(HEAD,GET))
	def showForm(@ModelAttribute("command") cmd: Appliable[Extension] with RequestExtensionCommandState): Mav = {
		val (assignment, module) = (cmd.assignment, cmd.module)

		if (!module.adminDepartment.canRequestExtension) {
			logger.info("Rejecting access to extension request screen as department does not allow extension requests")
			throw new PermissionDeniedException(user, Permissions.Extension.MakeRequest, assignment)
		} else {
			if (user.loggedIn){
				val existingRequest = assignment.findExtension(user.universityId)
				existingRequest.foreach(cmd.presetValues)
				val profile = profileService.getMemberByUser(user.apparentUser)
				// is this an edit of an existing request
				val isModification = existingRequest.isDefined && !existingRequest.get.isManual
				Mav("submit/extension_request",
					"profile" -> profile,
					"module" -> module,
					"assignment" -> assignment,
					"isClosed" -> assignment.isClosed,
					"department" -> module.adminDepartment,
					"isModification" -> isModification,
					"existingRequest" -> existingRequest.orNull,
					"command" -> cmd,
					"returnTo" -> getReturnTo("/coursework" + Routes.assignment(assignment))
				)
			} else {
				RedirectToSignin()
			}
		}
	}

	@RequestMapping(method=Array(POST))
	def persistExtensionRequest(@Valid @ModelAttribute("command") cmd: Appliable[Extension] with RequestExtensionCommandState, errors: Errors): Mav = {
		val (assignment, module) = (cmd.assignment, cmd.module)

		if (errors.hasErrors){
			showForm(cmd)
		} else {
			val extension = cmd.apply()
			val model = Mav("submit/extension_request_success",
				"module" -> module,
				"assignment" -> assignment
			)
			crumbed(model, module)
		}
	}
}
