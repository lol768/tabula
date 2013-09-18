package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StaffMember

class CreateMemberNoteCommandTest extends TestBase with Mockito {

	@Test
	def invalidMemberNote = withUser("cuscao") {

			val staffMember = new StaffMember
			staffMember.universityId = currentUser.universityId

		  val member = Fixtures.student(universityId = "12345")
		  val cmd = new CreateMemberNoteCommand(member, currentUser)

		  var errors = new BindException(cmd, "command")
		  cmd.validate(errors)
		  errors.hasErrors should be (true)
		  errors.getErrorCount should be (1)
		  errors.getFieldError.getField should be ("note")
		  errors.getFieldError.getCode should be ("profiles.memberNote.empty")

			cmd.note = " "
			cmd.validate(errors)
			errors.hasErrors should be (true)
			errors.getErrorCount should be (2)
	}

	@Test
	def validMemberNote = withUser("cuscao") {

		val staffMember = new StaffMember
		staffMember.universityId = currentUser.universityId

		val member = Fixtures.student(universityId = "12345")
		val cmd = new CreateMemberNoteCommand(member, currentUser)

		var errors = new BindException(cmd, "command")

		cmd.note = "   a note"
		cmd.validate(errors)
		errors.hasErrors should be(false)
	}

}
