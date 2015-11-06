package uk.ac.warwick.tabula.commands.coursework.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.Fixtures

class ArchiveAssignmentsTest  extends TestBase with Mockito {

	trait CommandTestSupport extends AssessmentServiceComponent {
		val assessmentService = mock[AssessmentService]
		def apply(): Seq[Assignment] = Seq()
	}

	trait Fixture {
		val department = Fixtures.department("bs")
		val module = Fixtures.module("bs101")

		val assignment = Fixtures.assignment("Essay 1")
		assignment.archive()
	}

	@Test
	def commandApply() {
		new Fixture {
			val command = new ArchiveAssignmentsCommand(department, Seq(module)) with CommandTestSupport

			command.assignments = Seq(assignment)
			assignment.isAlive should be(false)
			command.applyInternal()
			verify(command.assessmentService, times(1)).save(assignment)

			assignment.unarchive()
			assignment.isAlive should be(true)
			command.applyInternal()
			verify(command.assessmentService, times(2)).save(assignment)
			assignment.isAlive should be(false)

		}
	}

}
