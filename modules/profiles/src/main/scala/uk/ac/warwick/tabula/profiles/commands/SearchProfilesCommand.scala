package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.Student

class SearchProfilesCommand(val currentMember: Member, user: CurrentUser) extends AbstractSearchProfilesCommand(user, Student) {

	override def applyInternal() =
		if (validQuery) usercodeMatches ++ universityIdMatches ++ queryMatches
		else Seq()
		
	private def queryMatches = {
		val depts = (currentMember.affiliatedDepartments ++ moduleService.departmentsOwnedBy(currentMember.userId)).distinct
		profileService.findMembersByQuery(query, depts, userTypes, user.god)
	}
}
