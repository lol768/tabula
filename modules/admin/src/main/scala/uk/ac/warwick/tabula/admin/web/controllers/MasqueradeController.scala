package uk.ac.warwick.tabula.admin.web.controllers

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute

import org.springframework.web.bind.annotation.RequestMapping

import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.admin.commands.MasqueradeCommand
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.tabula.web.{Cookie, Mav}
import uk.ac.warwick.tabula.commands.Appliable

@Controller
@RequestMapping(Array("/masquerade"))
class MasqueradeController extends AdminController {

	type MasqueradeCommand = Appliable[Option[Cookie]]

	@ModelAttribute("masqueradeCommand") def command: MasqueradeCommand = MasqueradeCommand()

	@RequestMapping(method = Array(HEAD, GET))
	def form(@ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand): Mav = Mav("masquerade/form").crumbs(Breadcrumbs.Current("Masquerade"))

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("masqueradeCommand") cmd: MasqueradeCommand, response: HttpServletResponse): Mav = {
		for (cookie <- cmd.apply()) response.addCookie(cookie)
		Redirect("/masquerade")
	}

}