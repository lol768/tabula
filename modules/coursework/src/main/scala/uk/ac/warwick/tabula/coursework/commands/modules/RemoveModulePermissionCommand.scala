package uk.ac.warwick.tabula.coursework.commands.modules

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Module
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.springframework.validation.Errors
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.util.core.StringUtils
import uk.ac.warwick.spring.Wire

class RemoveModulePermissionCommand extends Command[Unit] {

	@BeanProperty var module: Module = _
	@BeanProperty var usercodes: JList[String] = _
	@BeanProperty val permissionType: String = "Participate"

	var userLookup = Wire.auto[UserLookupService]

	def work() {
		transactional() {
			for (user <- usercodes) module.participants.removeUser(user)
		}
	}

	def validate(errors: Errors) {
		if (usercodesEmpty) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else {
			for (code <- usercodes) {
				if (!module.participants.includes(code)) {
					errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
				}
			}
		}
	}

	private def usercodesEmpty = usercodes.find { StringUtils.hasText(_) }.isEmpty

	def describe(d: Description) = d.module(module).properties(
		"usercodes" -> usercodes.mkString(","),
		"type" -> permissionType)

}