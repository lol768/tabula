package uk.ac.warwick.tabula.attendance.commands.view

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

case class StudentReportCount(student: StudentMember, missed: Int, unrecorded: Int)

object ReportStudentsChoosePeriodCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new ReportStudentsChoosePeriodCommandInternal(department, academicYear)
			with ComposableCommand[Seq[StudentReportCount]]
			with AutowiringProfileServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with ReportStudentsChoosePeriodValidation
			with ReportStudentsChoosePeriodPermissions
			with ReportStudentsChoosePeriodCommandState
			with ReadOnly with Unaudited
}


class ReportStudentsChoosePeriodCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[StudentReportCount]] {

	self: TermServiceComponent with ReportStudentsChoosePeriodCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		studentReportCounts
	}

}

trait ReportStudentsChoosePeriodValidation extends SelfValidating {

	self: ReportStudentsChoosePeriodCommandState =>

	override def validate(errors: Errors) {
		if (!availablePeriods.filter(_._2).map(_._1).contains(period)) {
			errors.rejectValue("period", "attendanceMonitoringReport.invalidPeriod")
		}
	}

}

trait ReportStudentsChoosePeriodPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ReportStudentsChoosePeriodCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Report, department)
	}

}

trait ReportStudentsChoosePeriodCommandState extends FilterStudentsAttendanceCommandState with TaskBenchmarking {

	self: TermServiceComponent with AttendanceMonitoringServiceComponent =>

	// Only students whose enrolment department is this department
	lazy val allStudents = benchmarkTask("profileService.findAllStudentsByRestrictions") {
		profileService.findAllStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions(academicYear)
		).sortBy(s => (s.lastName, s.firstName))
	}

	lazy val studentPointMap = benchmarkTask("studentPointMap") {
		allStudents.map(s => s -> attendanceMonitoringService.listStudentsPoints(s, Option(department), academicYear)).toMap
	}

	lazy val termPoints = benchmarkTask("termPoints") {
		studentPointMap.values.flatten.toSeq.groupBy{ point =>
			termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString
		}.mapValues(_.distinct)
	}

	lazy val availablePeriods: Seq[(String, Boolean)] = benchmarkTask("availablePeriods") {
		val termsWithPoints = termPoints.keys.toSeq
		val thisTerm = {
			if (academicYear.startYear < AcademicYear.findAcademicYearContainingDate(DateTime.now, termService).startYear)
				TermService.orderedTermNames.last
			else
				termService.getTermFromDateIncludingVacations(DateTime.now).getTermTypeAsString
		}
		val thisTermIndex = TermService.orderedTermNames.zipWithIndex
			.find(_._1 == thisTerm).getOrElse(throw new ItemNotFoundException())._2
		val termsSoFarThisYear = TermService.orderedTermNames.slice(0, thisTermIndex + 1)
		val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(allStudents, academicYear)
		val termsToShow = termsSoFarThisYear.intersect(termsWithPoints)
		// Visible terms as those that are this term or before
		// Terms that can be selected are those that no selected student has been reported for
		termsToShow.map(term => term -> nonReportedTerms.contains(term))
	}

	lazy val studentReportCounts = {
		val relevantPoints = termPoints(period).intersect(studentPointMap.values.flatten.toSeq)
		val checkpoints = attendanceMonitoringService.getCheckpoints(relevantPoints, allStudents)
		allStudents.map { student => {
			// Points the student is taking that are in the given period
			val studentPoints = termPoints(period).intersect(studentPointMap(student))
			val unrecorded = studentPoints.count(point =>
				checkpoints.get(student).flatMap(_.get(point)).isEmpty
			)
			val missed = studentPoints.count(point =>
				checkpoints.get(student).flatMap(_.get(point)).exists(_.state == AttendanceState.MissedUnauthorised)
			)
			StudentReportCount(student, missed, unrecorded)
		}}.filter(_.missed > 0)
	}

	// Bind variables

	var period: String = _
}