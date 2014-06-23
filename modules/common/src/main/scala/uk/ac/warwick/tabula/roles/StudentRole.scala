package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._

case class StudentRole(department: model.Department) extends BuiltInRole(StudentRoleDefinition, department)

case object StudentRoleDefinition extends UnassignableBuiltInRoleDefinition {
	
	override def description = "Student"
		
	GrantsGlobalPermission(
		Profiles.Read.Core // As per discussion in TAB-753, anyone at the University can see anyone else's core information
	)
	
	GrantsScopelessPermission(
		UserPicker,
		MonitoringPointTemplates.View
	)

	GrantsScopedPermission(
		MemberNotes.Read,
		Profiles.Read.ModuleRegistration.Core,
		Profiles.Read.ModuleRegistration.Results
	)
}