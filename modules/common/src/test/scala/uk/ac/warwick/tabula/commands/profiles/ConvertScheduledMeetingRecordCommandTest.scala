package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ConvertScheduledMeetingRecordCommandTest extends TestBase with Mockito {

	trait Fixture {
		val meetingRecordDao = mock[MeetingRecordDao]
		val notificationService = mock[NotificationService]
		val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]

		var creator: StaffMember = Fixtures.staff(universityId = "12345")
		val student = Fixtures.student(universityId = "0123456")
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student)


		val createCommand = new Appliable[MeetingRecord] {
			override def apply(): MeetingRecord = {
				new MeetingRecord
			}
		}

		val scheduledMeetingRecord: ScheduledMeetingRecord = new ScheduledMeetingRecord()

		val command = new ConvertScheduledMeetingRecordCommand(creator, scheduledMeetingRecord) with ConvertScheduledMeetingRecordCommandSupport with MeetingRecordServiceComponent {
			val meetingRecordService: MeetingRecordService = mockMeetingRecordService
		}
		command.createCommand = createCommand
	}

	@Test
	def apply() { new Fixture {
		val newMeetingRecord = command.applyInternal()

		verify(mockMeetingRecordService, times(1)).purge(scheduledMeetingRecord)
	}}

	trait ConvertScheduledMeetingRecordCommandSupport extends FileAttachmentServiceComponent {
		def fileAttachmentService = mock[FileAttachmentService]
	}

}
