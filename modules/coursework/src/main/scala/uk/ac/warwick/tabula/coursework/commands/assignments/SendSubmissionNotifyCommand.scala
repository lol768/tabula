package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.Submission
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import org.joda.time.format.DateTimeFormat
import freemarker.template.Configuration
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserSettingsService

class SendSubmissionNotifyCommand (
		@BeanProperty var submission: Submission, 
		@BeanProperty var users: UserGroup) extends Command[Boolean] with ReadOnly with FreemarkerRendering with UnicodeEmails { 

	def this() = this(null, null)
	
	@BeanProperty var assignment: Assignment = Option(submission).map { _.assignment }.orNull
	@BeanProperty var module: Module = Option(assignment).map { _.module }.orNull
	
	var userLookup = Wire.auto[UserLookupService]
	var userSettings = Wire.auto[UserSettingsService]
	implicit var freemarker = Wire.auto[Configuration]
	var mailSender = Wire[WarwickMailSender]("studentMailSender")
	var replyAddress = Wire.property("${mail.noreply.to}")
	var fromAddress = Wire.property("${mail.exceptions.to}")
	
	val dateFormatter = DateTimeFormat.forPattern("d MMMM yyyy 'at' HH:mm:ss")
	
	def applyInternal() = {		
		for(userId <- users.includeUsers) {
			val user = userLookup.getUserByUserId(userId)
			if(canEmailUser(user)) sendMail(user)
		}
		true
	}
	
	def canEmailUser(user: User) : Boolean = {
		userSettings.getSetting(user, "alertsSubmission") match {
			case "allSubmissions" => true
			case "lateSubmissions" => submission.isLate || submission.isAuthorisedLate
			case _ => false
		}
	}
	
	
	def sendMail(user: User) = {
		Option(user.getEmail) match {
			case Some(email) => mailSender send messageFor(user)
			case None =>
		}
	}
		
	def messageFor(user: User) = createMessage(mailSender) { message =>
		val moduleCode = module.code.toUpperCase()
		val submissionTitle = if(submission.isAuthorisedLate) "Authorised Late Submission" 
								else if(submission.isLate) "Late Submission"
								else "Submission"			
									
		message.setFrom(fromAddress)
		message.setReplyTo(replyAddress)
		message.setTo(user.getEmail)
		message.setSubject(encodeSubject(moduleCode + ": " + submissionTitle))
		message.setText(renderToString("/WEB-INF/freemarker/emails/submissionnotify.ftl", Map(
			"submission" -> submission,
			"submissionDate" -> dateFormatter.print(submission.submittedDate),
			"assignment" -> assignment,
			"module" -> module,
			"path" -> Routes.assignment.receipt(assignment)
		)))
	}
	
	override def describe(d: Description) {
		d.assignment(assignment)
		.submission(submission)
	}
	
	
	
}