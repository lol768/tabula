package uk.ac.warwick.tabula.profiles.web.controllers.tutor


import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Notifies, Command, Description}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.helpers.Promises
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.helpers.Promises
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import org.springframework.web.bind.annotation.RequestParam
import org.joda.time.DateTime
import uk.ac.warwick.tabula.profiles.notifications.TutorChangeNotification
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.{FreemarkerTextRenderer, TextRenderer}
import scala._
import scala.Some
import scala.Some

class EditTutorCommand(val student: StudentMember, val currentTutor: Option[Member], val currentUser:User, val remove: Boolean)
	extends Command[Option[StudentRelationship]] with Notifies[StudentRelationship] with Promises {
	
	var profileService = Wire[ProfileService]

	var tutor: Member = _

	PermissionCheck(Permissions.Profiles.PersonalTutor.Update, student)

	// throw this request out if personal tutors can't be edited in Tabula for this department
	if (!student.studyDetails.studyDepartment.canEditPersonalTutors) {
		logger.info("Denying access to EditTutorCommand since student "
				+ student.studyDetails.sprCode
				+ " has a study department "
				+ "( " + student.studyDetails.studyDepartment.name
				+ ") with a personal tutor source setting of "
				+ student.studyDetails.studyDepartment.personalTutorSource + ".")
		throw new ItemNotFoundException()
	}

	val newTutor = promise { tutor }

	var notifyTutee: Boolean = false
	var notifyOldTutor: Boolean = false
	var notifyNewTutor: Boolean = false

	var modifiedRelationships: Seq[StudentRelationship] = Nil

	def applyInternal = {
		if (!currentTutor.isDefined) {
			// Brand new tutor
			val newRelationship = profileService.saveStudentRelationship(PersonalTutor, student.studyDetails.sprCode, tutor.universityId)

			modifiedRelationships = Seq(newRelationship)
			Some(newRelationship)
		} else if (currentTutor.get != tutor) {
			// Replacing the current tutor with a new one
			val currentRelationships = profileService.findCurrentRelationships(PersonalTutor, student.studyDetails.sprCode)
			
			// Is there an existing relationship for this tutor? 
			// Could happen if a student has two tutors, and we're trying to replace the second with the first
			currentRelationships.find(_.agent == tutor.universityId) match {
				case Some(existingRelationship) => {
					// Just return the existing relationship without any notifications
						Some(existingRelationship)
				}
				case _ => {
					// Find the relationship for the current tutor, and end it
					endTutorRelationship(currentRelationships)
					
					// Save the new relationship
					val newRelationship = profileService.saveStudentRelationship(PersonalTutor, student.studyDetails.sprCode, tutor.universityId)

					modifiedRelationships = Seq(newRelationship)
					Some(newRelationship)
				}
			}
		} else if (currentTutor.get == tutor && remove) {
				val currentRelationships = profileService.findCurrentRelationships(PersonalTutor, student.studyDetails.sprCode)
				endTutorRelationship(currentRelationships)
				modifiedRelationships = currentRelationships
				None
		} else {
				None
		}
	}

	def endTutorRelationship(currentRelationships: Seq[StudentRelationship]) {
		currentRelationships.find(_.agent == currentTutor.get.universityId) foreach { rel =>
			rel.endDate = DateTime.now
			profileService.saveOrUpdate(rel)
		}
	}

	override def describe(d: Description) = {
		val desc = d
		desc.property("student ID" -> student.universityId)
		desc.property("new tutor ID" -> tutor.universityId)
	}

	def emit: Seq[Notification[StudentRelationship]] = {

		val notifications = modifiedRelationships.flatMap(relationship => {

			val tuteeNotification:List[Notification[StudentRelationship]] = if(notifyTutee){
				val template = TutorChangeNotification.TuteeTemplate
				val recepient = relationship.studentMember.asSsoUser
				List(new TutorChangeNotification(relationship, currentUser, recepient, currentTutor, template) with FreemarkerTextRenderer)
			} else Nil

			val oldTutorNotification:List[Notification[StudentRelationship]] = if(notifyOldTutor){
				val notifications = currentTutor.map(oldTutor => {
					val template = TutorChangeNotification.OldTutorTemplate
					val recepient =  oldTutor.asSsoUser
					new TutorChangeNotification(relationship, currentUser, recepient, currentTutor, template) with FreemarkerTextRenderer
				})
				List(notifications).flatten
			} else Nil

			val newTutorNotification:List[Notification[StudentRelationship]] = if(notifyNewTutor){
				val notifications = relationship.agentMember.map(newTutor => {
					val template = TutorChangeNotification.NewTutorTemplate
					val recepient = newTutor.asSsoUser
					new TutorChangeNotification(relationship, currentUser, recepient, currentTutor, template) with FreemarkerTextRenderer
				})
				List(notifications).flatten
			} else Nil

			tuteeNotification ++ oldTutorNotification ++ newTutorNotification
		})

		notifications
	}
}

@Controller
@RequestMapping(Array("/tutor/{student}"))
class EditTutorController extends BaseController {
	var profileService = Wire.auto[ProfileService]

	@ModelAttribute("editTutorCommand")
	def editTutorCommand(@PathVariable("student") student: Member,
	                     @RequestParam(value="currentTutor", required=false)
	                     currentTutor: Member,
	                     @RequestParam(value="remove", required=false) remove: Boolean,
		                   user: CurrentUser) = student match {
		case student: StudentMember => new EditTutorCommand(student, Option(currentTutor), user.apparentUser, Option(remove).getOrElse(false))
		case _ => throw new ItemNotFoundException
	}

	// initial form display
	@RequestMapping(value = Array("/edit","/add"),method=Array(GET))
	def editTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest) = {
		Mav("tutor/edit/view",
			"student" -> cmd.student,
			"tutorToDisplay" -> cmd.currentTutor
		).noLayout()
	}
	

	@RequestMapping(value = Array("/edit", "/add"), method=Array(POST))
	def saveTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest ) = {
		val rel = cmd.apply()

		Mav("tutor/edit/view",
			"student" -> cmd.student,
			"tutorToDisplay" -> cmd.currentTutor
		)
	}
}
