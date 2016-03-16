package uk.ac.warwick.tabula.commands.attendance

import uk.ac.warwick.tabula.{MockUserLookup, AcademicYear, Fixtures, CurrentUser, TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringCheckpoint, MonitoringPointSet}
import uk.ac.warwick.tabula.data.model.{Department, Route}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.permissions.{Permissions, Permission}
import org.joda.time.DateTime
import org.mockito.Matchers
import uk.ac.warwick.tabula.data.convert.{MonitoringPointIdConverter, MemberUniversityIdConverter}
import org.springframework.web.bind.WebDataBinder
import org.springframework.core.convert.support.GenericConversionService
import uk.ac.warwick.util.termdates.Term
import uk.ac.warwick.tabula.commands.attendance.view.old.{SetMonitoringCheckpointState, SetMonitoringCheckpointCommandValidation, SetMonitoringCheckpointCommand}

class SetMonitoringPointsCommandTest extends TestBase with Mockito {

	val thisProfileService = mock[ProfileService]
	val thisUserLookup = new MockUserLookup
	val thisMonitoringPointService = mock[MonitoringPointService]

	trait CommandTestSupport extends SetMonitoringCheckpointState with SetMonitoringCheckpointCommandValidation
		with MonitoringPointServiceComponent with ProfileServiceComponent with SecurityServiceComponent with TermServiceComponent {
		val monitoringPointService = thisMonitoringPointService
		val profileService = thisProfileService
		val userLookup = thisUserLookup
		val securityService = mock[SecurityService]
		val termService = mock[TermService]
		def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route] = {
			Set()
		}
		def apply: Seq[MonitoringCheckpoint] = {
			Seq()
		}
	}

	trait Fixture {
		val memberConvertor = new MemberUniversityIdConverter
		memberConvertor.service = thisProfileService
		val monitoringPointConvertor = new MonitoringPointIdConverter
		monitoringPointConvertor.service = thisMonitoringPointService
		val conversionService = new GenericConversionService()
		conversionService.addConverter(memberConvertor)
		conversionService.addConverter(monitoringPointConvertor)

		val user = mock[CurrentUser]
		val dept = Fixtures.department("arc", "School of Architecture")
		val thisAcademicYear = AcademicYear(2013)
		val templatePoint = Fixtures.monitoringPoint("name", 1, 1)
		val templatePointSet = new MonitoringPointSet
		templatePointSet.academicYear = thisAcademicYear
		templatePoint.pointSet = templatePointSet

		val route = Fixtures.route("a100")
		val otherRoute = Fixtures.route("b100")

		val student1 = Fixtures.student("student1")
		student1.freshStudentCourseDetails.head.route = route
		thisProfileService.getMemberByUniversityId("student1") returns Option(student1)
		thisProfileService.getMemberByUniversityIdStaleOrFresh("student1") returns Option(student1)

		val monitoringPointSet1 = new MonitoringPointSet
		monitoringPointSet1.route = route
		monitoringPointSet1.academicYear = thisAcademicYear

		val pointSet1Point1 = Fixtures.monitoringPoint("name", 1, 1)
		pointSet1Point1.id = "11"
		pointSet1Point1.pointSet = monitoringPointSet1
		monitoringPointSet1.points.add(pointSet1Point1)
		thisMonitoringPointService.getPointById("11") returns Option(pointSet1Point1)

		val monitoringPointSet2 = new MonitoringPointSet
		monitoringPointSet2.route = otherRoute
		monitoringPointSet2.academicYear = thisAcademicYear

		val pointSet2Point1 = Fixtures.monitoringPoint("name", 1, 1)
		pointSet2Point1.id = "21"
		pointSet2Point1.pointSet = monitoringPointSet2
		monitoringPointSet2.points.add(pointSet2Point1)
		thisMonitoringPointService.getPointById("21") returns Option(pointSet2Point1)

		thisMonitoringPointService.getPointSetForStudent(student1, thisAcademicYear) returns Option(monitoringPointSet1)

		val term = mock[Term]
		term.getTermTypeAsString returns "Autumn"

	}

	@Test
	def onBindStudentsPopulated() { new Fixture {
		val command = new SetMonitoringCheckpointCommand(dept, templatePoint, user, JArrayList()) with CommandTestSupport
		command.onBind(null)
		command.studentsStateAsScala should not be null
	}}

	@Test
	def success() { new Fixture{
		val command = new SetMonitoringCheckpointCommand(dept, templatePoint, user, JArrayList()) with CommandTestSupport

		command.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(AcademicYear(2013))) returns 5
		command.termService.getTermFromAcademicWeekIncludingVacations(pointSet1Point1.validFromWeek, monitoringPointSet1.academicYear) returns term
		command.monitoringPointService.findNonReportedTerms(Seq(student1), monitoringPointSet1.academicYear) returns Seq("Autumn")

		command.studentsState = JHashMap(
			student1 -> JHashMap(pointSet1Point1 -> AttendanceState.Attended.asInstanceOf[AttendanceState])
		)
		var binder = new WebDataBinder(command, "command")
		binder.setConversionService(conversionService)
		command.onBind(null)
		var errors = binder.getBindingResult
		command.validate(errors)
		errors.hasFieldErrors should be (false)
		// TAB-2025
		verify(command.termService, times(0)).getTermFromAcademicWeek(any[Int], any[AcademicYear], any[Boolean])
	}}

	@Test def validateNoSuchPointForStudent() { new Fixture {
		val command = new SetMonitoringCheckpointCommand(dept, templatePoint, user, JArrayList()) with CommandTestSupport
		command.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(AcademicYear(2013))) returns 5
		command.studentsState = JHashMap(
			student1 -> JHashMap(pointSet2Point1 -> AttendanceState.Attended.asInstanceOf[AttendanceState])
		)
		var binder = new WebDataBinder(command, "command")
		binder.setConversionService(conversionService)
		command.onBind(null)
		var errors = binder.getBindingResult
		command.validate(errors)
		errors.hasFieldErrors should be (true)
		errors.getFieldError(s"studentsState[${student1.universityId}][${pointSet2Point1.id}]") should not be null
	}}

	@Test def validateBeforeValidFromAttended() { new Fixture {
		val command = new SetMonitoringCheckpointCommand(dept, templatePoint, user, JArrayList()) with CommandTestSupport
		command.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(AcademicYear(2013))) returns 5

		command.monitoringPointService.findNonReportedTerms(Seq(student1), monitoringPointSet1.academicYear) returns Seq("Spring")

		command.studentsState = JHashMap(
			student1 -> JHashMap(pointSet1Point1 -> AttendanceState.Attended.asInstanceOf[AttendanceState])
		)
		command.securityService.can(user, Permissions.MonitoringPoints.Record, route) returns true
		pointSet1Point1.validFromWeek = 10

		command.termService.getTermFromAcademicWeekIncludingVacations(pointSet1Point1.validFromWeek, monitoringPointSet1.academicYear) returns term

		var binder = new WebDataBinder(command, "command")
		binder.setConversionService(conversionService)
		command.onBind(null)
		var errors = binder.getBindingResult
		command.validate(errors)
		errors.hasFieldErrors should be (true)
		errors.getFieldError(s"studentsState[${student1.universityId}][${pointSet1Point1.id}]") should not be null
	}}

	@Test def validateBeforeValidFromMissedUnauthorised() { new Fixture {
		val command = new SetMonitoringCheckpointCommand(dept, templatePoint, user, JArrayList()) with CommandTestSupport
		command.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(AcademicYear(2013))) returns 5
		command.studentsState = JHashMap(
			student1 -> JHashMap(pointSet1Point1 -> AttendanceState.MissedUnauthorised.asInstanceOf[AttendanceState])
		)
		command.securityService.can(user, Permissions.MonitoringPoints.Record, route) returns true
		pointSet1Point1.validFromWeek = 10

		command.termService.getTermFromAcademicWeekIncludingVacations(pointSet1Point1.validFromWeek, monitoringPointSet1.academicYear) returns term
		command.monitoringPointService.findNonReportedTerms(Seq(student1), monitoringPointSet1.academicYear) returns Seq("Spring")

		var binder = new WebDataBinder(command, "command")
		binder.setConversionService(conversionService)
		command.onBind(null)
		var errors = binder.getBindingResult
		command.validate(errors)
		errors.hasFieldErrors should be (true)
		errors.getFieldError(s"studentsState[${student1.universityId}][${pointSet1Point1.id}]") should not be null
	}}

	@Test def validateBeforeValidFromMissedAuthorised() { new Fixture {
		val command = new SetMonitoringCheckpointCommand(dept, templatePoint, user, JArrayList()) with CommandTestSupport
		command.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(AcademicYear(2013))) returns 5
		command.studentsState = JHashMap(
			student1 -> JHashMap(pointSet1Point1 -> AttendanceState.MissedAuthorised.asInstanceOf[AttendanceState])
		)
		command.securityService.can(user, Permissions.MonitoringPoints.Record, route) returns true
		pointSet1Point1.validFromWeek = 10

		command.termService.getTermFromAcademicWeekIncludingVacations(pointSet1Point1.validFromWeek, monitoringPointSet1.academicYear) returns term
		command.monitoringPointService.findNonReportedTerms(Seq(student1), monitoringPointSet1.academicYear) returns Seq("Autumn")

		var binder = new WebDataBinder(command, "command")
		binder.setConversionService(conversionService)
		command.onBind(null)
		var errors = binder.getBindingResult
		command.validate(errors)
		errors.hasFieldErrors should be (false)
	}}

	@Test def validateBeforeValidFromNull() { new Fixture {
		val command = new SetMonitoringCheckpointCommand(dept, templatePoint, user, JArrayList()) with CommandTestSupport
		command.termService.getAcademicWeekForAcademicYear(any[DateTime], Matchers.eq(AcademicYear(2013))) returns 5
		command.studentsState = JHashMap(
			student1 -> JHashMap(pointSet1Point1 -> null.asInstanceOf[AttendanceState])
		)
		command.securityService.can(user, Permissions.MonitoringPoints.Record, route) returns true
		pointSet1Point1.validFromWeek = 10

		command.termService.getTermFromAcademicWeekIncludingVacations(pointSet1Point1.validFromWeek, monitoringPointSet1.academicYear) returns term
		command.monitoringPointService.findNonReportedTerms(Seq(student1), monitoringPointSet1.academicYear) returns Seq("Autumn")

		var binder = new WebDataBinder(command, "command")
		binder.setConversionService(conversionService)
		command.onBind(null)
		var errors = binder.getBindingResult
		command.validate(errors)
		errors.hasFieldErrors should be (false)
	}}
}