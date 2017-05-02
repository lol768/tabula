package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/new/{assignment}/markers"))
class ModifyAssignmentMarkersController extends CourseworkController {

	type ListMarkerAllocationsCommand = Appliable[MarkerAllocations] with ListMarkerAllocationsState
	type AssignMarkersCommand = Appliable[Assignment] with AssignMarkersState

	validatesSelf[SelfValidating]

	@ModelAttribute("assignMarkersCommand")
	def assignMarkersCommand(@PathVariable assignment: Assignment) = AssignMarkersCommand(mandatory(assignment))

	@ModelAttribute("listAllocationsCommand")
	def listAllocationsCommand(@PathVariable assignment: Assignment) = ListMarkerAllocationsCommand(mandatory(assignment))

	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav = {
		val module =  mandatory(assignMarkersCmd.assignment.module)
		val workflow = mandatory(assignMarkersCmd.assignment.cm2MarkingWorkflow)
		val existingAllocations = listAllocationsCmd.apply()

		Mav(s"$urlPrefix/admin/assignments/assignment_assign_markers",
			"module" -> module,
			"department" -> module.adminDepartment,
			"stages" -> workflow.allStages.groupBy(_.roleName).mapValues(_.map(_.name)),
			"state" -> existingAllocations
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers))
	def saveAndExit(
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav =  {
		assignMarkersCmd.apply()
		RedirectForce(Routes.home)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions))
	def submitAndAddSubmissions(
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@Valid @ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav = {
		val assignment = assignMarkersCmd.apply()
		RedirectForce(Routes.admin.assignment.createAddSubmissions(assignment))
	}
}
