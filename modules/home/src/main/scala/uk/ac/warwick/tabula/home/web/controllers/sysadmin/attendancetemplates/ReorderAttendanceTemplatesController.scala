package uk.ac.warwick.tabula.home.web.controllers.sysadmin.attendancetemplates

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.home.commands.sysadmin.attendancetemplates.ReorderAttendanceTemplatesCommand
import uk.ac.warwick.tabula.home.web.controllers.sysadmin.BaseSysadminController
import uk.ac.warwick.tabula.sysadmin.web.Routes

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates/reorder"))
class ReorderAttendanceTemplatesController extends BaseSysadminController {

	@ModelAttribute("command")
	def command = ReorderAttendanceTemplatesCommand()

	@RequestMapping
	def submit(@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringTemplate]]) = {
		cmd.apply()
		Redirect(Routes.AttendanceTemplates.list)
	}

}
