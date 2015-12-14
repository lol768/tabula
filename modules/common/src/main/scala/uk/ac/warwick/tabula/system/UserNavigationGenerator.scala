package uk.ac.warwick.tabula.system

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringCacheStrategyComponent, PermissionsService}
import uk.ac.warwick.tabula.services.{CourseAndRouteService, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.userlookup.{User, UserLookupInterface}
import uk.ac.warwick.util.cache.{CacheEntryFactory, Caches}

import scala.collection.JavaConverters._

case class UserNavigation(
	collapsed: String,
	expanded: String
) extends java.io.Serializable

trait UserNavigationGenerator {
	def apply(user: User, forceUpdate: Boolean = false): UserNavigation
}

object UserNavigationGeneratorImpl extends UserNavigationGenerator with AutowiredTextRendererComponent with AutowiringCacheStrategyComponent {

	final val NavigationTemplate = "/WEB-INF/freemarker/navigation.ftl"
	final val CacheName = "UserNavigation"
	final val CacheExpiryTime = 60 * 60 * 6 // 6 hours in seconds

	var moduleService = Wire[ModuleAndDepartmentService]
	var routeService = Wire[CourseAndRouteService]
	var permissionsService = Wire[PermissionsService]
	var userLookup = Wire[UserLookupInterface]

	private def render(user: CurrentUser): UserNavigation = {
		val canDeptAdmin = user.loggedIn && moduleService.departmentsWithPermission(user, Permissions.Department.Reports).nonEmpty
		val canAdmin = canDeptAdmin ||
			// Avoid doing too much work by just returning the first one of these that's true
			user.loggedIn && (
				moduleService.departmentsWithPermission(user, Permissions.Module.Administer).nonEmpty ||
				moduleService.departmentsWithPermission(user, Permissions.Route.Administer).nonEmpty ||
				moduleService.modulesWithPermission(user, Permissions.Module.Administer).nonEmpty ||
				routeService.routesWithPermission(user, Permissions.Route.Administer).nonEmpty
			)

		val canViewProfiles =
			user.isStaff ||
				user.isStudent ||
				permissionsService.getAllPermissionDefinitionsFor(user, Permissions.Profiles.ViewSearchResults).nonEmpty

		val canViewExams = user.isStaff && moduleService.getDepartmentByCode(user.apparentUser.getDepartmentCode).exists(_.uploadMarksToSits)

		val modelMap = Map(
			"user" -> user,
			"canAdmin" -> canAdmin,
			"canDeptAdmin" -> canDeptAdmin,
			"canViewProfiles" -> canViewProfiles,
			"canViewExams" -> canViewExams
		)
		UserNavigation(
			textRenderer.renderTemplate(NavigationTemplate, modelMap ++ Map("isCollapsed" -> true)),
			textRenderer.renderTemplate(NavigationTemplate, modelMap ++ Map("isCollapsed" -> false))
		)
	}

	val cacheEntryFactory = new CacheEntryFactory[String, UserNavigation] {
		def create(usercode: String): UserNavigation = {
			userLookup.getUserByUserId(usercode) match {
				case FoundUser(foundUser) =>
					render(new CurrentUser(foundUser, foundUser))
				case _ =>
					UserNavigation("", "")
			}
		}
		def create(keys: JList[String]): JMap[String, UserNavigation] = {
			JMap(keys.asScala.map(id => (id, create(id))): _*)
		}
		def isSupportsMultiLookups: Boolean = true
		def shouldBeCached(response: UserNavigation): Boolean = true
	}

	private lazy val navigationCache =
		Caches.newCache(CacheName, cacheEntryFactory, CacheExpiryTime, cacheStrategy)

	def apply(user: User, forceUpdate: Boolean = false) = {
		if (forceUpdate) {
			navigationCache.remove(user.getUserId)
			navigationCache.get(user.getUserId)
		} else {
			navigationCache.get(user.getUserId)
		}
	}

}