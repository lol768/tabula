package uk.ac.warwick.courses.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.data.model.Submission

class SubmissionIdConverter extends Converter[String, Submission] {

	@Autowired var service: AssignmentService = _

	override def convert(id: String) = service.getSubmission(id).orNull

}