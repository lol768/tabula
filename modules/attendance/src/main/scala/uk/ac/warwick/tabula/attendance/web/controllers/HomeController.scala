package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.commands.HomeCommand
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.web.Routes

/**
 * Displays the Attendance home screen.
 * Redirects to the the appropriate page if only one of the following is true:
 * * The user has a profile
 * * The user has view/record permissions on a single department
 * * The user has manage permissions on a single department
 * Otherwise they are shown the home page
 */
@Controller
@RequestMapping(Array("/"))
class HomeController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[(Boolean, Map[String, Set[Department]])]) = {
		cmd.apply() match {
			case (hasProfile, permissionsMap) => {
				if (hasProfile && permissionsMap("ManagePermissions").size == 0 && permissionsMap("ViewPermissions").size == 0)
					Redirect(Routes.profile())
				else if (!hasProfile && permissionsMap("ManagePermissions").size == 0 && permissionsMap("ViewPermissions").size == 1)
					Redirect(Routes.department.view(permissionsMap("ViewPermissions").head))
				else if (!hasProfile && permissionsMap("ManagePermissions").size == 1 && permissionsMap("ViewPermissions").size == 0)
					Redirect(Routes.department.manage(permissionsMap("ManagePermissions").head))
				else if (hasProfile || permissionsMap("ManagePermissions").size > 0 || permissionsMap("ViewPermissions").size > 0)
					Mav("home/home", "permissionMap" -> permissionsMap, "hasProfile" -> hasProfile)
				else Mav("home/nopermission")
			}
		}
	}

}