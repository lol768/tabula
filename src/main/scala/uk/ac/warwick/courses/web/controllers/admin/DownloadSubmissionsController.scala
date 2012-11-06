package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.commands.assignments.DownloadAllSubmissionsCommand
import uk.ac.warwick.courses.commands.assignments.DownloadSubmissionsCommand
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.spring.Wire

class DownloadSubmissionsController extends BaseController {

	var fileServer = Wire.auto[FileServer]

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions.zip"))
	def download(command: DownloadSubmissionsCommand, response: HttpServletResponse) {
		val (assignment, module, filename) = (command.assignment, command.module, command.filename)
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		command.apply { renderable =>
			fileServer.serve(renderable, response)
		}
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download-zip/{filename}"))
	def downloadAll(command: DownloadAllSubmissionsCommand, response: HttpServletResponse) {
		val (assignment, module, filename) = (command.assignment, command.module, command.filename)
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		command.apply { renderable =>
			fileServer.serve(renderable, response)
		}
	}

}