package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data.model.Assignment

case class Marker(assignment: Assignment) extends BuiltInRole(assignment) {
	
	GrantsPermissionFor(assignment,
		Feedback.Create,
		Marks.Create,
		Submission.Read
	)

}