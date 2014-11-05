package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.groups.commands.admin.{AdminSmallGroupsHomeCommand, ReleaseGroupSetCommandImpl, ReleasedSmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

@RequestMapping(Array("/admin/department/{department}/groups/release/{academicYear}"))
@Controller
class ReleaseAllSmallGroupSetsController extends GroupsController {

	@ModelAttribute("moduleList") def newViewModel(@PathVariable academicYear:AcademicYear):ModuleListViewModel={
		new ModuleListViewModel(academicYear)
	}

	@RequestMapping
	def form(@ModelAttribute("moduleList") model: ModuleListViewModel,
					 @PathVariable department:Department,
					 @PathVariable academicYear:AcademicYear,
					 showFlash:Boolean=false) = {
		Mav("admin/groups/bulk-release",
			"department"->department,
			"modules"->department.modules,
			"showFlash"->showFlash,
			"academicYear" -> academicYear
		).crumbs(Breadcrumbs.Department(department))
	}

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("moduleList") model: ModuleListViewModel, @PathVariable department:Department, @PathVariable academicYear:AcademicYear) = {
    model.createCommand(user.apparentUser).apply()
    Redirect(Routes.admin.release(department, academicYear), "batchReleaseSuccess"->true)
  }

	class ModuleListViewModel(val academicYear: AcademicYear) {
		var checkedModules: JList[Module] = JArrayList()
		var notifyStudents: JBoolean = true
		var notifyTutors: JBoolean = true
		var sendEmail: JBoolean = true

		def smallGroupSets() = {
			if (checkedModules == null) {
				// if  no modules are selected, spring binds null, not an empty list :-(
				Nil
			} else {
				checkedModules.asScala.flatMap(mod =>
					mod.groupSets.asScala.filter(_.academicYear == academicYear)
				)
			}
		}

		def createCommand(user: User): Appliable[Seq[ReleasedSmallGroupSet]] = {
			val command = new ReleaseGroupSetCommandImpl(smallGroupSets(), user)
			command.notifyStudents = notifyStudents
			command.notifyTutors = notifyTutors
			command.sendEmail = sendEmail
			command
		}
	}
}