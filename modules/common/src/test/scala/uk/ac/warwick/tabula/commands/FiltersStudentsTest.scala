package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.{Mockito, TestBase, JavaImports, Fixtures}
import uk.ac.warwick.tabula.data.model.{CourseType, ModeOfAttendance, Department, SitsStatus, Module, Route}
import uk.ac.warwick.tabula.services.ProfileService
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.JavaImports.JArrayList

class FiltersStudentsTest extends TestBase with Mockito {

	trait Fixture {
		val thisProfileService = mock[ProfileService]

		val dept = Fixtures.department("arc", "School of Architecture")
		val subDept = Fixtures.department("arc-ug", "Architecture Undergraduates")
		subDept.parent = dept
		dept.children.add(subDept)

		val mod1 = Fixtures.module("ac101", "Introduction to Architecture")
		val mod2 = Fixtures.module("ac102", "Architecture Basics")
		val mod3 = Fixtures.module("ac901", "Postgraduate Thesis")

		dept.modules.add(mod3)
		subDept.modules.add(mod2)
		subDept.modules.add(mod1)

		val route1 = Fixtures.route("a501", "Architecture BA")
		val route2 = Fixtures.route("a502", "Architecture BA with Intercalated year")
		val route3 = Fixtures.route("a9p1", "Architecture MA")

		dept.routes.add(route3)
		subDept.routes.add(route2)
		subDept.routes.add(route1)

		val sprF = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
		val sprP = Fixtures.sitsStatus("P", "Permanently Withdrawn", "Permanently Withdrawn")

		val moaFT = Fixtures.modeOfAttendance("F", "FT", "Full time")
		val moaPT = Fixtures.modeOfAttendance("P", "PT", "Part time")
	}

	@Test
	def initState() { new Fixture {
		thisProfileService.allSprStatuses(dept) returns Seq(sprF, sprP)
		thisProfileService.allModesOfAttendance(dept) returns Seq(moaFT, moaPT)

		val filter = new FiltersStudents {
			val department: Department = dept
			val courseTypes: JavaImports.JList[CourseType] = JArrayList()
			val modesOfAttendance: JavaImports.JList[ModeOfAttendance] = JArrayList()
			val defaultOrder: Seq[Order] = Seq()
			val sprStatuses: JavaImports.JList[SitsStatus] = JArrayList()
			val sortOrder: JavaImports.JList[Order] = JArrayList()
			val profileService: ProfileService = thisProfileService
			val yearsOfStudy: JavaImports.JList[JavaImports.JInteger] = JArrayList()
			val modules: JavaImports.JList[Module] = JArrayList()
			val routes: JavaImports.JList[Route] = JArrayList()
		}

		filter.allCourseTypes should be (CourseType.all)
		filter.allModesOfAttendance should be (Seq(moaFT, moaPT))
		filter.allModules should be (Seq(mod1, mod2, mod3)) // Order should be right through implicit ordering
		filter.allRoutes should be (Seq(route1, route2, route3)) // Order should be right through implicit ordering
		filter.allSprStatuses should be (Seq(sprF, sprP))
		filter.allYearsOfStudy should be (1 to FilterStudentsOrRelationships.MaxYearsOfStudy)
	}}

	@Test
	def serializeFilter() { new Fixture {
		thisProfileService.allSprStatuses(dept) returns Seq(sprF, sprP)
		thisProfileService.allModesOfAttendance(dept) returns Seq(moaFT, moaPT)

		val filter = new FiltersStudents {
			val department: Department = dept
			val courseTypes: JavaImports.JList[CourseType] = JArrayList(CourseType.UG)
			val modesOfAttendance: JavaImports.JList[ModeOfAttendance] = JArrayList(moaFT)
			val defaultOrder: Seq[Order] = Seq()
			val sprStatuses: JavaImports.JList[SitsStatus] = JArrayList(sprF)
			val sortOrder: JavaImports.JList[Order] = JArrayList()
			val profileService: ProfileService = thisProfileService
			val yearsOfStudy: JavaImports.JList[JavaImports.JInteger] = JArrayList(1)
			val modules: JavaImports.JList[Module] = JArrayList(mod1)
			val routes: JavaImports.JList[Route] = JArrayList(route1, route2)
		}

		val serialized = filter.serializeFilter
		serialized.contains("courseTypes=" + CourseType.UG.value) should be (true)
		serialized.contains("courseTypes=" + CourseType.PGT.value) should be (false)
		serialized.contains("modesOfAttendance=" + moaFT.code) should be (true)
		serialized.contains("modesOfAttendance=" + moaPT.code) should be (false)
		serialized.contains("sprStatuses=" + sprF.code) should be (true)
		serialized.contains("sprStatuses=" + sprP.code) should be (false)
		serialized.contains("yearsOfStudy=" + 1) should be (true)
		serialized.contains("yearsOfStudy=" + 2) should be (false)
		serialized.contains("modules=" + mod1.code) should be (true)
		serialized.contains("modules=" + mod2.code) should be (false)
		serialized.contains("routes=" + route1.code) should be (true)
		serialized.contains("routes=" + route2.code) should be (true)
		serialized.contains("routes=" + route3.code) should be (false)
	}}

	@Test
	def serializeFilterEmpty() { new Fixture {
		thisProfileService.allSprStatuses(dept) returns Seq(sprF, sprP)
		thisProfileService.allModesOfAttendance(dept) returns Seq(moaFT, moaPT)

		val filter = new FiltersStudents {
			val department: Department = dept
			val courseTypes: JavaImports.JList[CourseType] = JArrayList()
			val modesOfAttendance: JavaImports.JList[ModeOfAttendance] = JArrayList()
			val defaultOrder: Seq[Order] = Seq()
			val sprStatuses: JavaImports.JList[SitsStatus] = JArrayList()
			val sortOrder: JavaImports.JList[Order] = JArrayList()
			val profileService: ProfileService = thisProfileService
			val yearsOfStudy: JavaImports.JList[JavaImports.JInteger] = JArrayList()
			val modules: JavaImports.JList[Module] = JArrayList()
			val routes: JavaImports.JList[Route] = JArrayList()
		}

		val serialized = filter.serializeFilter
		serialized should not be null
		serialized should be ("")
	}}

}
