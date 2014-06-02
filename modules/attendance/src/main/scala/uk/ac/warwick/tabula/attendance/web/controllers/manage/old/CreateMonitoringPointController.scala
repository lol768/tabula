package uk.ac.warwick.tabula.attendance.web.controllers.manage.old

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.commands.Appliable
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.commands.manage.old.CreateMonitoringPointCommand

@Controller
@RequestMapping(Array("/manage/{dept}/2013/sets/{set}/edit/points/add"))
class CreateMonitoringPointController extends AbstractManageMonitoringPointController {

	@ModelAttribute("command")
	def createCommand(@PathVariable set: MonitoringPointSet) =
	 CreateMonitoringPointCommand(mandatory(set))

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPoint]) = {
	 Mav("manage/point/create_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPoint], errors: Errors) = {
	 if (errors.hasErrors) {
		 form(cmd)
	 } else {
		 cmd.apply()
		 Mav("manage/set/_monitoringPointsPersisted").noLayoutIf(ajax)
	 }
	}

 }
