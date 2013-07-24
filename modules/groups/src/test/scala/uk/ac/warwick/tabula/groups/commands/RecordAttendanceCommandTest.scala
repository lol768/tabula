package uk.ac.warwick.tabula.groups.commands

import scala.collection.JavaConverters._

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.services.{UserLookupComponent, SmallGroupServiceComponent, UserLookupService, SmallGroupService}
import uk.ac.warwick.userlookup.User

class RecordAttendanceCommandTest extends TestBase with Mockito {

	// Implements the dependencies declared by the command
	trait CommandTestSupport extends SmallGroupServiceComponent with UserLookupComponent {
		val smallGroupService = mock[SmallGroupService]
		val userLookup = mock[UserLookupService]
		def apply() {}
	}

	@Test
	def commandApply() {
		val event = mock[SmallGroupEvent]
		val week = 1
		val user = new User("abcde")

		val command = new RecordAttendanceCommand(event, week) with CommandTestSupport
		//command.userLookup.getUsersByUserIds(Seq("abcde").asJava) returns (Map("abcde" -> user).asJava)
		command.attendees.add("abcde")
		command.applyInternal()

		there was no(command.userLookup).getUsersByUserIds(Seq("abcde").asJava)
		there was one(command.smallGroupService).updateAttendance(event, week, Seq("abcde"))
	}

}
