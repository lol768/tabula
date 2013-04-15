package uk.ac.warwick.tabula.data

import org.hibernate.criterion._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.permissions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.permissions.Permission
import scala.reflect.ClassTag
import uk.ac.warwick.userlookup.User

trait PermissionsDao {
	def saveOrUpdate(roleDefinition: CustomRoleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_])
	def saveOrUpdate(role: GrantedRole[_])
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](scope: A): Seq[GrantedRole[A]]
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](scope: A): Seq[GrantedPermission[A]]
	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, customRoleDefinition: CustomRoleDefinition): Option[GrantedRole[A]]
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, builtInRoleDefinition: BuiltInRoleDefinition): Option[GrantedRole[A]]
	
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]]
	
	def getGrantedRolesForUser(user: User): Seq[GrantedRole[_]]
	def getGrantedRolesForWebgroup(groupName: String): Seq[GrantedRole[_]]
	
	def getGrantedPermissionsForUser(user: User): Seq[GrantedPermission[_]]
	def getGrantedPermissionsForWebgroup(groupName: String): Seq[GrantedPermission[_]]
}

@Repository
class PermissionsDaoImpl extends PermissionsDao with Daoisms {
	import Restrictions._
	import Order._
	
	def saveOrUpdate(roleDefinition: CustomRoleDefinition) = session.saveOrUpdate(roleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_]) = session.saveOrUpdate(permission)
	def saveOrUpdate(role: GrantedRole[_]) = session.saveOrUpdate(role)
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](scope: A) = canDefineRoleSeq(scope) {
		session.newCriteria[GrantedRole[A]]
					 .add(is("scope", scope))
					 .seq
	}
	
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](scope: A) = canDefinePermissionSeq(scope) {
		session.newCriteria[GrantedPermission[A]]
					 .add(is("scope", scope))
					 .seq
	}
					 
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, customRoleDefinition: CustomRoleDefinition) = canDefineRole(scope) { 
		session.newCriteria[GrantedRole[A]]
					 .add(is("scope", scope))
					 .add(is("customRoleDefinition", customRoleDefinition))
					 .seq.headOption
	}
					 
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, builtInRoleDefinition: BuiltInRoleDefinition) = canDefineRole(scope) {
		session.newCriteria[GrantedRole[A]]
					 .add(is("scope", scope))
					 .add(is("builtInRoleDefinition", builtInRoleDefinition))
					 .seq.headOption
	}
					 
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean) = canDefinePermission(scope) {
		session.newCriteria[GrantedPermission[A]]
					 .add(is("scope", scope))
					 .add(is("permission", permission))
					 .add(is("overrideType", overrideType))
					 .seq.headOption
	}
					 
	private def canDefinePermissionSeq[A <: PermissionsTarget](scope: A)(f: => Seq[GrantedPermission[A]]) = {
		if (GrantedPermission.canDefineFor(scope)) f
		else Seq()
	}
					 
	private def canDefineRoleSeq[A <: PermissionsTarget](scope: A)(f: => Seq[GrantedRole[A]]) = {
		if (GrantedRole.canDefineFor(scope)) f
		else Seq()
	}
					 
	private def canDefinePermission[A <: PermissionsTarget](scope: A)(f: => Option[GrantedPermission[A]]) = {
		if (GrantedPermission.canDefineFor(scope)) f
		else None
	}
					 
	private def canDefineRole[A <: PermissionsTarget](scope: A)(f: => Option[GrantedRole[A]]) = {
		if (GrantedRole.canDefineFor(scope)) f
		else None
	}
	
	def getGrantedRolesForUser(user: User) =
		session.newQuery[GrantedRole[_]]("""
				select distinct r
				from GrantedRole r
				where 
					(
						r.users.universityIds = false and 
						((:userId in elements(r.users.staticIncludeUsers)
						or :userId in elements(r.users.includeUsers))
						and :userId not in elements(r.users.excludeUsers))
					) or (
						r.users.universityIds = true and 
						((:universityId in elements(r.users.staticIncludeUsers)
						or :universityId in elements(r.users.includeUsers))
						and :universityId not in elements(r.users.excludeUsers))
					)
		""")
			.setString("universityId", user.getWarwickId())
			.setString("userId", user.getUserId())
			.seq
	
	def getGrantedRolesForWebgroup(groupName: String) =
		session.newCriteria[GrantedRole[_]]
			.createAlias("users", "users")
			.add(is("users.baseWebgroup", groupName))
			.seq
	
	def getGrantedPermissionsForUser(user: User) =
		session.newQuery[GrantedPermission[_]]("""
				select distinct r
				from GrantedPermission r
				where 
					(
						r.users.universityIds = false and 
						((:userId in elements(r.users.staticIncludeUsers)
						or :userId in elements(r.users.includeUsers))
						and :userId not in elements(r.users.excludeUsers))
					) or (
						r.users.universityIds = true and 
						((:universityId in elements(r.users.staticIncludeUsers)
						or :universityId in elements(r.users.includeUsers))
						and :universityId not in elements(r.users.excludeUsers))
					)
		""")
			.setString("universityId", user.getWarwickId())
			.setString("userId", user.getUserId())
			.seq
	
	
	def getGrantedPermissionsForWebgroup(groupName: String) =
		session.newCriteria[GrantedPermission[_]]
			.createAlias("users", "users")
			.add(is("users.baseWebgroup", groupName))
			.seq
					
}
