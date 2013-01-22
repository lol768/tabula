package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.{ Module, Assignment }
import uk.ac.warwick.tabula.commands.{ Description, Command }
import reflect.BeanProperty
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.beans.factory.annotation.{ Value, Autowired, Configurable }
import uk.ac.warwick.userlookup.{ User, UserLookup }
import collection.JavaConversions._
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import freemarker.template.Configuration
import javax.annotation.Resource
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.UnicodeEmails

/**
 * Sends a message to one or more admins to let them know that the current
 * user thinks they should have access to an assignment.
 */
class RequestAssignmentAccessCommand(user: CurrentUser) extends Command[Unit] with FreemarkerRendering with UnicodeEmails {

	@BeanProperty var module: Module = _
	@BeanProperty var assignment: Assignment = _

	var userLookup = Wire.auto[UserLookupService]
	implicit var freemarker = Wire.auto[Configuration]
	var mailSender = Wire[WarwickMailSender]("mailSender")
	var replyAddress = Wire.property("${mail.noreply.to}")
	var fromAddress = Wire.property("${mail.exceptions.to}")

	override def applyInternal() {
		val admins = module.department.owners

		val adminUsers = userLookup.getUsersByUserIds(seqAsJavaList(admins.members))
		val manageAssignmentUrl = Routes.admin.assignment.edit(assignment)

		for ((usercode, admin) <- adminUsers if admin.isFoundUser) {
			val messageText = renderToString("/WEB-INF/freemarker/emails/requestassignmentaccess.ftl", Map(
				"assignment" -> assignment,
				"student" -> user,
				"admin" -> admin,
				"path" -> manageAssignmentUrl))
			val message = createMessage(mailSender) { message => 
				val moduleCode = module.code.toUpperCase
				message.setFrom(fromAddress)
				message.setReplyTo(replyAddress)
				message.setTo(admin.getEmail)
				message.setSubject(encodeSubject(moduleCode + ": Access request"))
				message.setText(messageText)
			}

			mailSender.send(message)
		}

	}

	// describe the thing that's happening.
	override def describe(d: Description) {
		d.assignment(assignment)
	}
}
