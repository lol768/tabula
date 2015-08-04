package uk.ac.warwick.tabula.commands.home

import java.util.concurrent.Future

import freemarker.template.{Configuration, Template}
import org.springframework.validation.{Errors, ValidationUtils}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.util.mail.WarwickMailSender

class AppCommentCommand(user: CurrentUser) extends Command[Future[JBoolean]] with FreemarkerRendering with UnicodeEmails with SelfValidating with Public {

	var mailSender = Wire[WarwickMailSender]("mailSender")
	var adminMailAddress = Wire.property("${mail.admin.to}")
	var freemarker = Wire.auto[Configuration]
	
	lazy val template: Template = freemarker.getTemplate("/WEB-INF/freemarker/emails/appfeedback.ftl")
	
	var componentName: String = _

	var message: String = _
	//	var pleaseRespond:Boolean =_
	var usercode: String = _
	var name: String = _
	var email: String = _
	var currentPage: String = _
	var browser: String = _
	var os: String = _
	var resolution: String = _
	var ipAddress: String = _
	
	if (user != null && user.loggedIn) {
		if (!usercode.hasText) usercode = user.apparentId
		if (!name.hasText) name = user.fullName
		if (!email.hasText) email = user.email
	}

	def applyInternal() = {
		val mail = createMessage(mailSender) { mail => 
			mail setTo adminMailAddress
			mail setFrom adminMailAddress
			mail setSubject encodeSubject("Tabula feedback")
			mail setText generateText
		}

		mailSender send mail
	}

	def generateText = renderToString(template, Map(
		"user" -> user,
		"info" -> this))

	def validate(errors: Errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "message", "NotEmpty")
	}

	def describe(d: Description) {}

	override def describeResult(d: Description) = d.properties(
		"name" -> name,
		"email" -> email,
		"message" -> message)

}