package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula._
import actions._
import data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import org.springframework.web.bind.annotation._
import org.springframework.stereotype._
import collection.JavaConversions._

@Controller
@RequestMapping(Array("/module/{module}/"))
class ModuleController extends CourseworkController {

	hideDeletedItems

	@RequestMapping
	def viewModule(@PathVariable module: Module) = {
		mustBeAbleTo(View(mandatory(module)))
		Mav("submit/module",
			"module" -> module,
			"assignments" -> module.assignments
				.filterNot { _.deleted }
				.sortBy { _.closeDate }
				.reverse)
	}

}
