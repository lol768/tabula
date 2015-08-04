package uk.ac.warwick.tabula.jobs.zips

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.services.{AutowiringSubmissionServiceComponent, AutowiringZipServiceComponent}
import collection.JavaConverters._

object SubmissionZipFileJob {
	val identifier = "submission-zip-file"
	val zipFileName = "submissions.zip"
	val minimumSubmissions = 10
	val SubmissionsKey = "submissions"

	def apply(submissions: Seq[String]) = JobPrototype(identifier, Map(
		SubmissionsKey -> submissions.asJava
	))
}

@Component
class SubmissionZipFileJob extends ZipFileJob with AutowiringZipServiceComponent with AutowiringSubmissionServiceComponent {

	override val identifier = SubmissionZipFileJob.identifier
	override val zipFileName = SubmissionZipFileJob.zipFileName
	override val itemDescription = "submissions"

	override def run(implicit job: JobInstance): Unit = new Runner(job).run()

	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		def run(): Unit = {
			val submissions = job.getStrings(SubmissionZipFileJob.SubmissionsKey).flatMap(submissionService.getSubmission)

			updateProgress(0)
			updateStatus("Initialising")

			val zipFile = zipService.getSomeSubmissionsZip(submissions, updateZipProgress)
			job.setString(ZipFileJob.ZipFilePathKey, zipFile.getPath)

			updateProgress(100)
			job.succeeded = true
		}
	}
}