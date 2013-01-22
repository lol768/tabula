package uk.ac.warwick.tabula.system.exceptions

import org.junit.Test
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.web.Uri
import javax.mail.internet.MimeMultipart
import uk.ac.warwick.util.mail.WarwickMailSender
import javax.mail.internet.MimeMessage
import uk.ac.warwick.tabula.Mockito
import javax.mail.Session
import java.util.Properties

class EmailingExceptionHandlerTest extends TestBase with Mockito {

	@Test def renderEmail {
		val user = new User("cusebr")
		val uri = "https://tabula.warwick.ac.uk/web/power/flight?super=magic"
	 	val currentUser = new CurrentUser(user, user)
		val info = new RequestInfo(currentUser, Uri.parse(uri))		
		val request = testRequest(uri)
		request.setMethod("GET")
		request.setParameter("mode", "powerEgg")
		request.setParameter("groups", Array("group1","group2"))
		request.addHeader("X-Requested-With", "Coconuts")
		val context = ExceptionContext("1", new RuntimeException("An egg cracked"), Some(request))
		RequestInfo.use(info) {
			val handler = new EmailingExceptionHandler
			handler.freemarker = newFreemarkerConfiguration
			
			val mailSender = mock[WarwickMailSender]
	   
			val session = Session.getDefaultInstance(new Properties)
			val mimeMessage = new MimeMessage(session)	   
			mailSender.createMimeMessage() returns mimeMessage
			
			handler.mailSender = mailSender
			handler.recipient = "exceptions@warwick.ac.uk"
			handler.afterPropertiesSet
			
			val message = handler.makeEmail(context)
			val text = message.getContent match {
				case string: String => string
				case multipart: MimeMultipart => multipart.getBodyPart(0).getContent.toString
			}
			
			text should include ("time=")
			text should include ("info.requestedUri="+uri)
			text should include ("request.requestURI="+uri)
			text should include ("request.method=GET")
			text should include ("request.params[mode]=[powerEgg]")
			text should include ("RuntimeException: An egg cracked")
			text should include ("X-Requested-With: Coconuts")
		}
	}
	
}