package uk.ac.warwick.tabula.web.controllers.reports.profiles

import java.io.File
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.reports.profiles.ProfileExportReportCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.jobs.reports.ProfileExportJob
import uk.ac.warwick.tabula.services.fileserver.{RenderableZip, FileServer}
import uk.ac.warwick.tabula.services.jobs.{AutowiringJobServiceComponent, JobInstance}
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/profiles/export/report"))
class ProfileExportReportController extends ReportsController with AutowiringJobServiceComponent {

	var fileServer = Wire.auto[FileServer]

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		ProfileExportReportCommand(department, academicYear, user)

	@RequestMapping(params = Array("!jobId"))
	def generateReport(
		@Valid @ModelAttribute("command") cmd: Appliable[JobInstance], errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		if(errors.hasErrors) {
			Mav("reports/profiles/export").crumbs(
				ReportsBreadcrumbs.Home.Department(department),
				ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear),
				ReportsBreadcrumbs.Profiles.Home(department, academicYear)
			)
		} else {
			val jobId = cmd.apply().id
			Mav("reports/profiles/export", "jobId" -> jobId).crumbs(
				ReportsBreadcrumbs.Home.Department(department),
				ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear),
				ReportsBreadcrumbs.Profiles.Home(department, academicYear)
			)
		}
	}

	@RequestMapping(params = Array("jobId"))
	def checkProgress(@RequestParam jobId: String) = {
		jobService.getInstance(jobId) match {
			case Some(job: JobInstance) => Mav(new JSONView(Map(
				"progress" -> job.progress.toString,
				"status" -> job.status,
				"succeeded" -> job.succeeded
			)))
			case _ => throw new ItemNotFoundException()
		}

	}

	@RequestMapping(value = Array("/zip"), params = Array("jobId"))
	def serveZip(@RequestParam jobId: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		jobService.getInstance(jobId) match {
			case Some(job: JobInstance) =>
				new File(job.getString(ProfileExportJob.ZipFilePathKey)) match {
					case zipFile if zipFile.exists() =>
						fileServer.serve(new RenderableZip(zipFile), Some(s"profile-export.zip"))
					case _ => throw new ItemNotFoundException()
				}
			case _ => throw new ItemNotFoundException()
		}
	}


}