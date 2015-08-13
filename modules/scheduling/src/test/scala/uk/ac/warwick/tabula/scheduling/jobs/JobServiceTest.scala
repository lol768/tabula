package uk.ac.warwick.tabula.scheduling.jobs

import uk.ac.warwick.tabula._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.tabula.services.jobs.JobInstanceImpl
import uk.ac.warwick.tabula.jobs.JobPrototype

class JobServiceTest extends AppContextTestBase {

	@Autowired var jobService: JobService = _
	
	@Test def containsTurnitin() {
		jobService.jobs.size should (be > 1)
		jobService.jobs map (_.identifier) should contain ("turnitin-submit")
	}

	@Test def containsTurnitinLti() {
		jobService.jobs.size should (be > 1)
		jobService.jobs map (_.identifier) should contain ("turnitin-submit-lti")
	}
	
	@Test def unknownJobType() {
		jobService.jobs map (_.identifier) should not contain "unknown-job-type"
		
		val instance = JobInstanceImpl.fromPrototype(JobPrototype("unknown", Map()))
		
		jobService.processInstance(instance)
		
		// Check that the flags have not actually been updated.
		withClue("Started") { instance.started should be {false} }
		withClue("Finished") { instance.finished should be {false} }
		withClue("Succeeded") { instance.succeeded should be {false} }
	}
}