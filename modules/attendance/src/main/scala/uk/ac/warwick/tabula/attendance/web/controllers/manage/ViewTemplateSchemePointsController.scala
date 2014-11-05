package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.attendance.commands.manage.FindPointsResult
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringTemplate}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.web.controllers.{HasMonthNames, AttendanceController}
import uk.ac.warwick.tabula.attendance.commands.GroupsPoints
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService


@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/addpoints/template/{templateScheme}"))
class ViewTemplateSchemePointsController extends AttendanceController with HasMonthNames with GroupsPoints with AutowiringTermServiceComponent {

	var attendanceService = Wire.auto[AttendanceMonitoringService]

	@ModelAttribute("command")
	def command(
		@PathVariable templateScheme: AttendanceMonitoringTemplate,
		@PathVariable academicYear: AcademicYear,
		@PathVariable department: Department
	) = {
		DepartmentFindPointsResult(mandatory(department), getGroupedPointsFromTemplate(mandatory(templateScheme), mandatory(academicYear)))
	}

	@RequestMapping
	def getTemplateSelection(
		@ModelAttribute("command") cmd: DepartmentFindPointsResult,
		@PathVariable templateScheme: AttendanceMonitoringTemplate,
		@PathVariable department: Department
	)	= {
		Mav("manage/_displayfindpointresults",
			"findResult" -> cmd.pointResult,
			"templateScheme" -> templateScheme,
			"command" -> cmd
		).noLayoutIf(ajax)
	}


	def getGroupedPointsFromTemplate(templateScheme: AttendanceMonitoringTemplate, academicYear: AcademicYear): FindPointsResult = {
		val points = attendanceService.generatePointsFromTemplateScheme(templateScheme, academicYear)
		templateScheme.pointStyle match {
			case AttendanceMonitoringPointStyle.Week => FindPointsResult(groupByTerm(points), Map(), Map())
			case AttendanceMonitoringPointStyle.Date => FindPointsResult(Map(), groupByMonth(points), Map())
			case _ => FindPointsResult(groupByTerm(points), groupByMonth(points), Map())
		}
	}
}

case class DepartmentFindPointsResult(department: Department, pointResult: FindPointsResult)