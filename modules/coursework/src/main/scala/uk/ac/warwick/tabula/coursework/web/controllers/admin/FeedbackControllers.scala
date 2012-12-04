package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.coursework.commands.feedback._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.tabula.services.fileserver.FileServer

@Controller
class DownloadFeedback extends CourseworkController {
	var feedbackDao = Wire.auto[FeedbackDao]
	var fileServer = Wire.auto[FileServer]

	@RequestMapping( value = Array("/admin/module/{module}/assignments/{assignment}/feedback/download/{feedbackId}/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable feedbackId: String, @PathVariable filename: String, response: HttpServletResponse) {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))

		feedbackDao.getFeedback(feedbackId) match {
			case Some(feedback) => {
				mustBeLinked(feedback, assignment)
				val renderable = new AdminGetSingleFeedbackCommand(feedback).apply()
				fileServer.serve(renderable, response)
			}
			case None => throw new ItemNotFoundException
		}
	}
	
	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedbacks.zip"))
    def getSelected(command: DownloadSelectedFeedbackCommand, response: HttpServletResponse) {
        val (assignment, module, filename) = (command.assignment, command.module, command.filename)
        mustBeLinked(assignment, module)
        mustBeAbleTo(Participate(module))
        command.apply { renderable =>
            fileServer.serve(renderable, response)
        }
    }   
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/download-zip/{filename}"))
class DownloadAllFeedback extends CourseworkController {
	var fileServer = Wire.auto[FileServer]
	
	@RequestMapping
	def download(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable filename: String, response: HttpServletResponse) {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		val renderable = new AdminGetAllFeedbackCommand(assignment).apply()
		fileServer.serve(renderable, response)
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/list"))
class ListFeedback extends CourseworkController {
	var auditIndexService = Wire.auto[AuditEventIndexService]

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(@PathVariable module: Module, @PathVariable assignment: Assignment) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		Mav("admin/assignments/feedback/list",
			"whoDownloaded" -> auditIndexService.whoDownloadedFeedback(assignment))
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
}

