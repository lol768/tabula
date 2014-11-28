package uk.ac.warwick.tabula.reports.commands.attendancemonitoring

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.reports.commands.attendancemonitoring.AllAttendanceReportCommand.AllAttendanceReportCommandResult
import uk.ac.warwick.tabula.reports.commands.{ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}

object AllAttendanceReportCommand {

	type AllAttendanceReportCommandResult = Map[AttendanceMonitoringStudentData, Map[AttendanceMonitoringPoint, AttendanceState]]
	def apply(
		department: Department,
		academicYear: AcademicYear,
		filter: AllAttendanceReportCommandResult => AllAttendanceReportCommandResult
	) =
		new AllAttendanceReportCommandInternal(department, academicYear, filter)
			with AutowiringProfileServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[AllAttendanceReportCommandResult]
			with ReportPermissions
			with AllAttendanceReportCommandState
			with ReadOnly with Unaudited
}

class AllAttendanceReportCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	val filter : AllAttendanceReportCommandResult => AllAttendanceReportCommandResult
)
	extends CommandInternal[AllAttendanceReportCommandResult] with TaskBenchmarking {

	self: ProfileServiceComponent with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		val allStudentData = benchmarkTask("allStudentData") {
			profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments(department, Seq(), academicYear)
		}
		val studentPointMap = benchmarkTask("studentPointMap") {
			allStudentData.map(studentData => studentData -> attendanceMonitoringService.listStudentsPoints(studentData, department, academicYear))
				.filter(_._2.nonEmpty)
		}.toMap
		val checkpointMap = benchmarkTask("checkpointMap") {
			attendanceMonitoringService.getAllCheckpointData(studentPointMap.values.flatten.toSeq.distinct).groupBy(_.point)
		}

		val result = benchmarkTask("result") {
			studentPointMap.map { case (studentData, points) =>
				studentData -> points.map(point => point -> checkpointMap.get(point).flatMap(
					checkpoints => checkpoints.find(_.universityId == studentData.universityId).map(_.state)).getOrElse(AttendanceState.NotRecorded)
				).toMap
			}
		}
		filter(result)
	}

}

trait AllAttendanceReportCommandState extends ReportCommandState {
}
