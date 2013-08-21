package uk.ac.warwick.tabula.coursework.commands.assignments

import org.joda.time.DateTimeConstants
import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import uk.ac.warwick.tabula.services.UserLookupService
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.UserGroup

// scalastyle:off magic.number
class ModifyAssignmentCommandTest extends AppContextTestBase with Mockito {

	val userLookup = mock[UserLookupService]
	userLookup.getUsersByUserIds(any[JList[String]]) answers { case ids: JList[String @unchecked] =>
		val users = ids.asScala.map(id=>(id,new User(id)))
		JHashMap(users:_*)
	}

	@Test def validateCloseDate = transactional { t =>
		// TAB-236
		val f = MyFixtures()

		val cmd = new AddAssignmentCommand(f.module)
		val errors = new BindException(cmd, "command")

		// No error, close date after open date
		cmd.openDate = new DateTime(2012, DateTimeConstants.JANUARY, 10, 0, 0)
		cmd.closeDate = cmd.openDate.plusDays(1)
		cmd.validate(errors)
		errors.getErrorCount() should be (0)

		// Close date is before open date; but open ended so still no error
		cmd.closeDate = cmd.openDate.minusDays(1)
		cmd.openEnded = true
		cmd.validate(errors)
		errors.getErrorCount() should be (0)

		// But if we're not open ended
		cmd.openEnded = false
		cmd.validate(errors)
		errors.getErrorCount() should be (1)
		withClue("correct error code") { errors.getGlobalError().getCode() should be ("closeDate.early") }
	}

	@Test def includeAndExcludeUsers = transactional {
		t =>
			val f = MyFixtures()
			val cmd = new EditAssignmentCommand(f.module, f.assignment)
			cmd.members match {
				case ug: UserGroup => ug.userLookup = userLookup
				case _ => fail("Expected to be able to set the userlookup on the usergroup.")
			}

			// have one user, add a new one and check re-add does nothing
			cmd.members.add(new User("already"))
			cmd.includeUsers = JList("custard", "already")
			cmd.afterBind()
			cmd.members.users.size should be(2)
			cmd.members.excludes.size should be(0)

			// remove one
			cmd.excludeUsers = JList("already")
			cmd.afterBind()
			cmd.members.users.size should be(1)
			cmd.members.excludes.size should be(0)

			// now exclude (blacklist) it
			cmd.excludeUsers = JList("already")
			cmd.afterBind()
			cmd.members.users.size should be(1)
			cmd.members.excludes.size should be(1)

			// now unexclude it
			cmd.includeUsers = JList("already")
			cmd.afterBind()
			cmd.members.users.size should be(1)
			cmd.members.excludes.size should be(0)
	}


	case class MyFixtures() {
		val module = Fixtures.module(code="ls101")
		val assignment = Fixtures.assignment("test")
		assignment.module = module
		module.assignments.add(assignment)
		session.save(module)
		session.save(assignment)
	}

}