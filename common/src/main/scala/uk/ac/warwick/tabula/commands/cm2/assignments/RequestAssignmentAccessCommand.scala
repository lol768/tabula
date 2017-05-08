package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Command, Description, Notifies}
import uk.ac.warwick.tabula.data.model.notifications.cm2.Cm2RequestAssignmentAccessNotification
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Notification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.userlookup.User

/**
 * Sends a message to one or more admins to let them know that the current
 * user thinks they should have access to an assignment.
 */
class RequestAssignmentAccessCommand(module: Module, assignment: Assignment, user: CurrentUser) extends Command[Seq[User]]
	with Notifies[Seq[User], Assignment] with FreemarkerRendering with UnicodeEmails with Public {

	mustBeLinked(mandatory(assignment), mandatory(module))

	def admins: Seq[User] = {
		// lookup the admin users - used to determine the recipients  for notifications
		module.adminDepartment.owners.users.filter(admin => admin.isFoundUser && admin.getEmail.hasText).toSeq
	}

	// Returns the Seq of admin users
	override def applyInternal(): Seq[User] = admins

	override def describe(d: Description) {
		d.assignment(assignment)
	}

	def emit(admins: Seq[User]): Seq[Cm2RequestAssignmentAccessNotification] = {
		Seq(Notification.init(new Cm2RequestAssignmentAccessNotification, user.apparentUser, Seq(assignment)))
	}
}