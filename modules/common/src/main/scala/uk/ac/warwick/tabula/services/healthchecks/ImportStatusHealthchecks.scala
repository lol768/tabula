package uk.ac.warwick.tabula.services.healthchecks

import humanize.Humanize._
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventQueryService

import scala.concurrent.Await
import scala.concurrent.duration._

abstract class AbstractImportStatusHealthcheck extends ServiceHealthcheckProvider {

	def WarningThreshold: Duration
	def ErrorThreshold: Duration
	def HealthcheckName: String

	/**
		* Fetch a list of audit events, most recent first, relating to this import
		*/
	protected def auditEvents: Seq[AuditEvent]

	@Scheduled(fixedRate = 60 * 1000) // 1 minute
	def run(): Unit = transactional(readOnly = true) {
		val imports = auditEvents

		// Get the last one that's successful
		val lastSuccessful = imports.find { event => !event.hadError && !event.isIncomplete }

		// Do we have a current running import?
		val isRunning = imports.headOption.filter(_.isIncomplete)

		// Did the last import fail
		val lastFailed = imports.find(!_.isIncomplete).filter(_.hadError)

		val status =
			if (!lastSuccessful.exists(_.eventDate.plusMillis(ErrorThreshold.toMillis.toInt).isAfterNow))
				ServiceHealthcheck.Status.Error
			else if (!lastSuccessful.exists(_.eventDate.plusMillis(WarningThreshold.toMillis.toInt).isAfterNow) || lastFailed.nonEmpty)
				ServiceHealthcheck.Status.Warning
			else
				ServiceHealthcheck.Status.Okay

		val successMessage =
			lastSuccessful.map { event => s"Last successful import ${naturalTime(event.eventDate.toDate)}" }

		val runningMessage =
			isRunning.map { event => s"import started ${naturalTime(event.eventDate.toDate)}" }

		val failedMessage =
			lastFailed.map { event => s"last import failed ${naturalTime(event.eventDate.toDate)}" }

		val message = Seq(
			successMessage.orElse(Some("No successful import found")),
			runningMessage,
			failedMessage
		).flatten.mkString(", ")

		val lastSuccessfulHoursAgo: Double =
			lastSuccessful.map { event =>
				val d = new org.joda.time.Duration(event.eventDate, DateTime.now)
				d.toStandardSeconds.getSeconds / 3600.0
			}.getOrElse(0)

		update(ServiceHealthcheck(
			name = HealthcheckName,
			status = status,
			testedAt = DateTime.now,
			message = message,
			performanceData = Seq(
				ServiceHealthcheck.PerformanceData("last_successful_hours", lastSuccessfulHoursAgo, WarningThreshold.toHours, ErrorThreshold.toHours)
			)
		))
	}

}

@Component
@Profile(Array("scheduling"))
class AcademicDataImportStatusHealthcheck extends AbstractImportStatusHealthcheck {

	// Warn if no successful import for 1 day, critical if no import for past day and a half
	override val WarningThreshold = 24.hours
	override val ErrorThreshold = 36.hours
	override val HealthcheckName = "import-academic"

	override protected def auditEvents: Seq[AuditEvent] = {
		val queryService = Wire[AuditEventQueryService]
		Await.result(queryService.query("eventType:ImportAcademicInformation", 0, 50), 1.minute)
	}

}

@Component
@Profile(Array("scheduling"))
class ProfileImportStatusHealthcheck extends AbstractImportStatusHealthcheck {

	// Warn if no successful import for 1 and a half days, critical if no import for past 2 days
	override val WarningThreshold = 36.hours
	override val ErrorThreshold = 48.hours
	override val HealthcheckName = "import-profiles"

	override protected def auditEvents: Seq[AuditEvent] = {
		val queryService = Wire[AuditEventQueryService]
		Await.result(queryService.query("eventType:ImportProfiles", 0, 50), 1.minute)
			.filter { event =>
				event.data == "{\"deptCode\":null}" || event.data == "{\"deptCode\":\"\"}"
			}
	}

}

@Component
@Profile(Array("scheduling"))
class AssignmentImportStatusHealthcheck extends AbstractImportStatusHealthcheck {

	// Warn if no successful import for 1 and a half days, critical if no import for past 2 days
	override val WarningThreshold = 36.hours
	override val ErrorThreshold = 48.hours
	override val HealthcheckName = "import-assignments"

	override protected def auditEvents: Seq[AuditEvent] = {
		val queryService = Wire[AuditEventQueryService]
		Await.result(queryService.query("eventType:ImportAssignments", 0, 50), 1.minute)
	}

}