package uk.ac.warwick.tabula.web.controllers.coursework

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, MemberOrUser}
import uk.ac.warwick.tabula.commands.coursework.assignments.CourseworkHomepageCommand.CourseworkHomepageInformation
import uk.ac.warwick.tabula.commands.coursework.assignments.{CourseworkHomepageActivityPageletCommand, CourseworkHomepageCommand}
import uk.ac.warwick.tabula.services.ActivityService.PagedActivities

@Controller class HomeController extends CourseworkController {

	hideDeletedItems

	@ModelAttribute("command") def command(user: CurrentUser) = CourseworkHomepageCommand(user)

	@RequestMapping(Array("/coursework")) def home(@ModelAttribute("command") cmd: Appliable[Option[CourseworkHomepageInformation]], user: CurrentUser) =
		cmd.apply() match {
			case Some(info) =>
				Mav("coursework/home/view",
					"student" -> MemberOrUser(user.profile, user.apparentUser),
					"enrolledAssignments" -> info.enrolledAssignments,
					"historicAssignments" -> info.historicAssignments,
					"assignmentsForMarking" -> info.assignmentsForMarking,
					"ownedDepartments" -> info.ownedDepartments,
					"ownedModule" -> info.ownedModules,
					"ownedModuleDepartments" -> info.ownedModules.map { _.adminDepartment },
					"activities" -> info.activities,
					"ajax" -> ajax)
			case _ => Mav("coursework/home/view")
		}
}

@Controller class HomeActivitiesPageletController extends CourseworkController {

	hideDeletedItems

	@ModelAttribute("command") def command(
		user: CurrentUser,
		@PathVariable lastUpdatedDate: Long) =
			CourseworkHomepageActivityPageletCommand(user, new DateTime(lastUpdatedDate))

	@RequestMapping(Array("/coursework/api/activity/pagelet/{lastUpdatedDate}"))
	def pagelet(@ModelAttribute("command") cmd: Appliable[Option[PagedActivities]]) = {
		try {
			cmd.apply() match {
				case Some(pagedActivities) =>
					Mav("coursework/home/activities",
						"activities" -> pagedActivities,
						"async" -> true).noLayout()
				case _ => Mav("coursework/home/empty").noLayout()
			}
		} catch {
			case e: IllegalStateException =>
				Mav("coursework/home/activities",
				"expired" -> true).noLayout()
		}
	}
}