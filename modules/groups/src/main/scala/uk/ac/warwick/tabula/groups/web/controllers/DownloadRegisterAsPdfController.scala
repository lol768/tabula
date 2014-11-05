package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.groups.commands.{DownloadRegisterAsPdfCommand, DownloadRegisterAsPdfCommandState}
import uk.ac.warwick.tabula.web.views.PDFView

@RequestMapping(value = Array("/event/{event}/register.pdf"))
@Controller
class DownloadRegisterAsPdfController extends GroupsController {

	type DownloadRegisterAsPdfCommand = Appliable[PDFView] with DownloadRegisterAsPdfCommandState

	@ModelAttribute
	def command(@PathVariable event: SmallGroupEvent, @RequestParam week: Int): DownloadRegisterAsPdfCommand
	= DownloadRegisterAsPdfCommand(event, week)

	@RequestMapping
	def downloadAsPdf(@ModelAttribute command: DownloadRegisterAsPdfCommand) = command.apply()

}