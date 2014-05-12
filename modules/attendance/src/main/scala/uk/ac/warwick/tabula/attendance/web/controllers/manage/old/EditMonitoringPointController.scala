package uk.ac.warwick.tabula.attendance.web.controllers.manage.old

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.commands.Appliable
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.attendance.commands.manage.old.{EditMonitoringPointState, EditMonitoringPointCommand}

@Controller
@RequestMapping(Array("/manage/{dept}/2013/sets/add/points/edit/{pointIndex}"))
class EditMonitoringPointController extends AbstractManageMonitoringPointController {

	@ModelAttribute("command")
	def createCommand(@PathVariable dept: Department, @PathVariable pointIndex: Int) = EditMonitoringPointCommand(dept, pointIndex)

	@RequestMapping(method=Array(POST), params = Array("form"))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPoint] with EditMonitoringPointState) = {
		cmd.copyFrom(cmd.pointIndex)
		Mav("manage/point/edit_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPoint] with EditMonitoringPointState, errors: Errors) = {
		if (errors.hasErrors) {
			Mav("manage/point/edit_form").noLayoutIf(ajax)
		} else {
			cmd.apply()
			Mav("manage/set/_monitoringPoints").noLayoutIf(ajax)
		}
	}

}
