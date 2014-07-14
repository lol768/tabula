package uk.ac.warwick.tabula.home.commands.sysadmin.attendancetemplates

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.services.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}

object ListAttendanceTemplatesCommand {
	def apply() =
		new ListAttendanceTemplatesCommandInternal
			with ComposableCommand[Seq[AttendanceMonitoringTemplate]]
			with AutowiringAttendanceMonitoringServiceComponent
			with AttendanceTemplatePermissions
			with ReadOnly with Unaudited
}


class ListAttendanceTemplatesCommandInternal extends CommandInternal[Seq[AttendanceMonitoringTemplate]] {

	self: AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		attendanceMonitoringService.listAllTemplateSchemes
	}

}
