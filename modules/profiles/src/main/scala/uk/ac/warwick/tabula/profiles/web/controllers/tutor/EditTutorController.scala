package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import scala.reflect.BeanProperty

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.PersonalTutor
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.profiles.commands.SearchTutorsCommand
import uk.ac.warwick.tabula.profiles.commands.TutorChangeNotifierCommand
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.controllers.BaseController

class EditTutorCommand(val student: Member) extends Command[Option[StudentRelationship]] {

	PermissionCheck(Permissions.Profiles.PersonalTutor.Update, student)

	@BeanProperty var tutorUniId: String = null
	
	@BeanProperty var storeTutor: Boolean = false
	
	var profileService = Wire.auto[ProfileService]
	
	def currentTutor = profileService.getPersonalTutor(student)
	
	@BeanProperty val notifyCommand = new TutorChangeNotifierCommand(student, currentTutor)
	
	def applyInternal = {
		if (!currentTutor.isDefined || !currentTutor.get.universityId.equals(tutorUniId)) {
			// it's a real change
			notifyCommand.oldTutor = currentTutor
			val newRelationship = profileService.saveStudentRelationship(PersonalTutor, student.sprCode, tutorUniId)
			notifyCommand.apply()
			Some(newRelationship)
		} else {
			None
		}
	}

	override def describe(d: Description) = d.property("student ID" -> student.universityId).property("new tutor ID" -> tutorUniId)
}

@Controller
@RequestMapping(Array("/tutor/{student}/edit"))
class EditTutorController extends BaseController {
	var profileService = Wire.auto[ProfileService]
	
	@ModelAttribute("searchTutorsCommand") def searchTutorsCommand =
		restricted(new SearchTutorsCommand(user)) orNull
	
	@ModelAttribute("editTutorCommand")
	def editTutorCommand(@PathVariable("student") student: Member) = new EditTutorCommand(student)

	// initial form display
	@RequestMapping(params = Array("!tutorUniId"))
	def editTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest) = {
		Mav("tutor/edit/view",
			"studentUniId" -> cmd.student.universityId,
			"tutorToDisplay" -> cmd.currentTutor,
			"displayOptionToSave" -> false)
	}

	// now we've got a tutor id to display, but it's not time to save it yet
	@RequestMapping(params = Array("tutorUniId", "!storeTutor"))
	def displayPickedTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest) = {

		val pickedTutor = profileService.getMemberByUniversityId(cmd.tutorUniId)

		Mav("tutor/edit/view",
			"studentUniId" -> cmd.student.universityId,
			"tutorToDisplay" -> pickedTutor,
			"pickedTutor" -> pickedTutor,
			"displayOptionToSave" -> (!cmd.currentTutor.isDefined || !cmd.currentTutor.get.universityId.equals(cmd.tutorUniId)))
	}

	@RequestMapping(params=Array("tutorUniId", "storeTutor"), method=Array(POST))
	def savePickedTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest ) = {
		
		val rel = cmd.apply()
		
		Mav("tutor/edit/view", 
			"studentUniId" -> cmd.student.universityId, 
			"tutorToDisplay" -> cmd.currentTutor,
			"displayOptionToSave" -> false
		)
	}
}
