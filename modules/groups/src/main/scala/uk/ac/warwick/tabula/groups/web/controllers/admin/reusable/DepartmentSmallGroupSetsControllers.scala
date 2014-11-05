package uk.ac.warwick.tabula.groups.web.controllers.admin.reusable

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, DepartmentSmallGroupSet, SmallGroupAllocationMethod, WeekRange}
import uk.ac.warwick.tabula.groups.commands.admin.reusable._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor

trait DepartmentSmallGroupSetsController extends GroupsController {

	validatesSelf[SelfValidating]
	
	@ModelAttribute("academicYearChoices") def academicYearChoices =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2)

	@ModelAttribute("ManageDepartmentSmallGroupsMappingParameters") def params = ManageDepartmentSmallGroupsMappingParameters

	override final def binding[A](binder: WebDataBinder, cmd: A) {
		binder.registerCustomEditor(classOf[SmallGroupAllocationMethod], new AbstractPropertyEditor[SmallGroupAllocationMethod] {
			override def fromString(code: String) = SmallGroupAllocationMethod.fromDatabase(code)			
			override def toString(method: SmallGroupAllocationMethod) = method.dbValue
		})
	}
	
}

@RequestMapping(Array("/admin/department/{department}/groups/reusable/new"))
@Controller
class CreateDepartmentSmallGroupSetController extends DepartmentSmallGroupSetsController {

	type CreateDepartmentSmallGroupSetCommand = Appliable[DepartmentSmallGroupSet] with CreateDepartmentSmallGroupSetCommandState

	@ModelAttribute("createDepartmentSmallGroupSetCommand") def cmd(@PathVariable("department") department: Department): CreateDepartmentSmallGroupSetCommand =
		ModifyDepartmentSmallGroupSetCommand.create(department)
		
	@RequestMapping
	def form(@ModelAttribute("createDepartmentSmallGroupSetCommand") cmd: CreateDepartmentSmallGroupSetCommand) = {
		Mav("admin/groups/reusable/new").crumbs(Breadcrumbs.Department(cmd.department))
	}
	
	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("createDepartmentSmallGroupSetCommand") cmd: CreateDepartmentSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			cmd.apply()
			Redirect(Routes.admin.reusable(cmd.department))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAddStudents))
	def submitAndAddStudents(@Valid @ModelAttribute("createDepartmentSmallGroupSetCommand") cmd: CreateDepartmentSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.reusable.createAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAddGroups))
	def submitAndAddGroups(@Valid @ModelAttribute("createDepartmentSmallGroupSetCommand") cmd: CreateDepartmentSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.reusable.createAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAllocate))
	def submitAndAllocate(@Valid @ModelAttribute("createDepartmentSmallGroupSetCommand") cmd: CreateDepartmentSmallGroupSetCommand, errors: Errors) = {
		if (errors.hasErrors) form(cmd)
		else {
			val set = cmd.apply()
			RedirectForce(Routes.admin.reusable.createAllocate(set))
		}
	}
}

@RequestMapping(Array("/admin/department/{department}/groups/reusable/edit/{smallGroupSet}"))
@Controller
class EditDepartmentSmallGroupSetController extends DepartmentSmallGroupSetsController {

	type EditDepartmentSmallGroupSetCommand = Appliable[DepartmentSmallGroupSet] with EditDepartmentSmallGroupSetCommandState

	@ModelAttribute("editDepartmentSmallGroupSetCommand") def cmd(@PathVariable("department") department: Department, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): EditDepartmentSmallGroupSetCommand =
		ModifyDepartmentSmallGroupSetCommand.edit(department, set)

	@RequestMapping
	def form(@ModelAttribute("editDepartmentSmallGroupSetCommand") cmd: EditDepartmentSmallGroupSetCommand, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet) = {
		Mav("admin/groups/reusable/edit").crumbs(Breadcrumbs.DepartmentForYear(set.department, set.academicYear))
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("editDepartmentSmallGroupSetCommand") cmd: EditDepartmentSmallGroupSetCommand, errors: Errors, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet) = {
		if (errors.hasErrors) form(cmd, set)
		else {
			cmd.apply()
			Redirect(Routes.admin.reusable(set.department))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAddStudents))
	def submitAndAddStudents(@Valid @ModelAttribute("editDepartmentSmallGroupSetCommand") cmd: EditDepartmentSmallGroupSetCommand, errors: Errors, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet) = {
		if (errors.hasErrors) form(cmd, set)
		else {
			cmd.apply()
			RedirectForce(Routes.admin.reusable.editAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups))
	def submitAndAddGroups(@Valid @ModelAttribute("editDepartmentSmallGroupSetCommand") cmd: EditDepartmentSmallGroupSetCommand, errors: Errors, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet) = {
		if (errors.hasErrors) form(cmd, set)
		else {
			cmd.apply()
			RedirectForce(Routes.admin.reusable.editAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAllocate))
	def submitAndAllocate(@Valid @ModelAttribute("editDepartmentSmallGroupSetCommand") cmd: EditDepartmentSmallGroupSetCommand, errors: Errors, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet) = {
		if (errors.hasErrors) form(cmd, set)
		else {
			cmd.apply()
			RedirectForce(Routes.admin.reusable.editAllocate(set))
		}
	}
}

@RequestMapping(Array("/admin/department/{department}/groups/reusable/delete/{smallGroupSet}"))
@Controller
class DeleteDepartmentSmallGroupSetController extends GroupsController {

	validatesSelf[SelfValidating]
	type DeleteDepartmentSmallGroupSetCommand = Appliable[DepartmentSmallGroupSet] with DeleteDepartmentSmallGroupSetValidation

	@ModelAttribute("command")
	def cmd(@PathVariable("department") department: Department, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): DeleteDepartmentSmallGroupSetCommand =
		DeleteDepartmentSmallGroupSetCommand(department, set)

	@RequestMapping
	def form(@PathVariable("department") department: Department) =
		Mav("admin/groups/reusable/delete").crumbs(Breadcrumbs.Department(department))

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: DeleteDepartmentSmallGroupSetCommand, errors: Errors, @PathVariable("department") department: Department) =
		if (errors.hasErrors) form(department)
		else {
			cmd.apply()
			Redirect(Routes.admin.reusable(department))
		}

}