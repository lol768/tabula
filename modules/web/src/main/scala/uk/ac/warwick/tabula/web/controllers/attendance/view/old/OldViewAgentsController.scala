package uk.ac.warwick.tabula.web.controllers.attendance.view.old

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.view.old.{ViewAgentsCommand, ViewAgentsResult}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}

@Controller
@RequestMapping(Array("/attendance/view/{department}/2013/agents/{relationshipType}"))
class OldViewAgentsController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	) =
		ViewAgentsCommand(department, relationshipType, Option(AcademicYear(2013)))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[ViewAgentsResult]], @PathVariable department: Department) = {
		Mav("attendance/home/agents", "agents" -> cmd.apply()).crumbs(Breadcrumbs.Old.ViewDepartment(department))
	}

}