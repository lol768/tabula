package uk.ac.warwick.tabula.attendance.web.controllers.check

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.attendance.commands.check.CheckMeetingRecordCheckpointCommand
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{MeetingFormat, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/check/meeting"))
class CheckMeetingRecordCheckpointController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@RequestParam student: StudentMember,
		@RequestParam relationshipType: StudentRelationshipType,
		@RequestParam meetingFormat: MeetingFormat,
		@RequestParam meetingDate: DateTime
	) =
		CheckMeetingRecordCheckpointCommand(
			mandatory(student),
			mandatory(relationshipType),
			mandatory(meetingFormat),
			mandatory(meetingDate)
		)

	@RequestMapping
	def render(@ModelAttribute("command") cmd: Appliable[Boolean]) =
		Mav(
			new JSONView(
				Map(
					"willCheckpointBeCreated" -> cmd.apply()
				)
			)
		)

}
