package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.attendance.commands.EditMonitoringPointSetCommand
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.Department

@Controller
@RequestMapping(value=Array("/manage/{dept}/sets/{set}/edit"))
class EditMonitoringPointSetController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createCommand(@PathVariable set: MonitoringPointSet) =
		EditMonitoringPointSetCommand(set)

	@RequestMapping(method=Array(GET,HEAD))
	def form(@PathVariable dept: Department, @ModelAttribute("command") cmd: Appliable[Unit]) = {
		cmd.apply()
		Mav("manage/set/edit_form").crumbs(Breadcrumbs.ManagingDepartment(dept))
	}

}
