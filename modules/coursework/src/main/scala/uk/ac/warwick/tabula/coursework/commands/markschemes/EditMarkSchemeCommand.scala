package uk.ac.warwick.tabula.coursework.commands.markschemes

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import reflect.BeanProperty
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MarkSchemeDao

/** Edit an existing markscheme. */
class EditMarkSchemeCommand(department: Department, val markScheme: MarkScheme) extends ModifyMarkSchemeCommand(department) {

	var dao = Wire.auto[MarkSchemeDao]

	def hasExistingSubmissions: Boolean = dao.getAssignmentsUsingMarkScheme(markScheme).exists(!_.submissions.isEmpty)

	// fill in the properties on construction
	copyFrom(markScheme)

	def contextSpecificValidation(errors:Errors){
		if (hasExistingSubmissions){
			/* TODO - reinstate when other options become available
			if (markScheme.studentsChooseMarker != studentsChooseMarker)
				errors.rejectValue("studentsChooseMarker", "markScheme.studentsChooseMarker.submissionsExist")
		  */
			if (markScheme.studentsChooseMarker){
				val existingMarkers = markScheme.firstMarkers.includeUsers.toSet
				val newMarkers = firstMarkers.toSet
				// if newMarkers is not a super set of existingMarker, markers have been removed.
				if (!(existingMarkers -- newMarkers).isEmpty){
					errors.rejectValue("firstMarkers", "markScheme.firstMarkers.cannotRemoveMarkers")
				}
			}
		}
	}

	def applyInternal() = {
		transactional() {
			this.copyTo(markScheme)
			session.update(markScheme)
			markScheme
		}
	}
	
	def currentMarkScheme = Some(markScheme)

	override def validate(errors: Errors) {
		super.validate(errors)
	}

	def describe(d: Description) = d.department(department).markScheme(markScheme)
}