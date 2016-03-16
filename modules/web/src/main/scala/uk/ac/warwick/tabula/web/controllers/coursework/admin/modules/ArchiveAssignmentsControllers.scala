package uk.ac.warwick.tabula.web.controllers.coursework.admin.modules

import scala.collection.JavaConverters._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.coursework.CourseworkController
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.commands.coursework.assignments.ArchiveAssignmentsCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser

@Controller
@RequestMapping(value = Array("/coursework/admin/module/{module}/archive-assignments"))
class ArchiveModuleAssignmentsController extends CourseworkController with UnarchivedAssignmentsMap {

	@ModelAttribute
	def archiveAssignmentsCommand(@PathVariable module: Module) = ArchiveAssignmentsCommand(module.adminDepartment, Seq(module))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable module: Module, cmd: ArchiveAssignmentsCommand) = {
		Mav("coursework/admin/modules/archive_assignments",
			"title" -> module.name,
			"cancel" -> Routes.admin.module(module),
			"map" -> moduleAssignmentMap(cmd.modules)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: ArchiveAssignmentsCommand, @PathVariable module: Module, errors: Errors, user: CurrentUser) = {
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

}

@Controller
@RequestMapping(value = Array("/coursework/admin/department/{department}/archive-assignments"))
class ArchiveDepartmentAssignmentsController extends CourseworkController with UnarchivedAssignmentsMap {

	@ModelAttribute
	def archiveAssignmentsCommand(@PathVariable department: Department) = {
		val modules = department.modules.asScala.filter(_.assignments.asScala.exists(_.isAlive))
		ArchiveAssignmentsCommand(department, modules)
	}



	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable department: Department, cmd: ArchiveAssignmentsCommand) = {
		Mav("coursework/admin/modules/archive_assignments",
			"title" -> department.name,
			"cancel" -> Routes.admin.department(department),
			"map" -> moduleAssignmentMap(cmd.modules),
			"showSubHeadings" -> true
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: ArchiveAssignmentsCommand, @PathVariable department: Department, errors: Errors, user: CurrentUser) = {
		cmd.apply()
		Redirect(Routes.admin.department(department))
	}
}