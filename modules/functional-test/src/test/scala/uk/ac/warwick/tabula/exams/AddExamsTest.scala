package uk.ac.warwick.tabula.exams

import org.openqa.selenium.By
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

class AddExamsTest extends ExamFixtures
	with GivenWhenThen
	with BeforeAndAfterEach {

	"Department admin" should "be offered a link to their department" in as(P.Admin1) {

			Given("The exams home page")
			pageTitle should be("Tabula - Exams Management")

			Then("The user should be shown a link to their department exams")
			click on linkText("Go to the Test Services admin page")
			pageSource contains "Test Services" should be {true}

			And("By default none will be shown until clicking the 'Show' button")
			click on (linkText("Show"))

			Then("The the user should be able to select the 'Create new exam' option from the 'Manage' dropdown")
			var info = getModuleInfo("XXX01")
			click on info.findElement(By.className("module-manage-button")).findElement(By.partialLinkText("Manage"))

			val createNewExam = info.findElement(By.partialLinkText("Create new exam"))
			eventually {
				createNewExam.isDisplayed should be {true}
			}
			click on createNewExam

			Then("This should show the create exam page")
			pageSource contains "Create exam for" should be {true}
			pageSource contains "XXX01" should be {true}

			Then("The user should be able to enter a name and create the exam")
			textField("name").value = "Module-XXX01-Exam1"
			submit()

			Then("The user should be returned to the department module page")
			pageSource contains "Test Services" should be {true}
			pageSource contains "XXX01" should be {true}

			Then("Clicking on the module XXX01 should reveal the new exam")
			info = getModuleInfo("XXX01")
			click on info
			eventually {
				pageSource contains "Module-XXX01-Exam1" should be {true}
			}
	}

	def getModuleInfo(moduleCode: String) = {
		val moduleInfoBlocks = findAll(className("module-info"))

		if (moduleInfoBlocks.isEmpty)
			throw new TestFailedException("No module-info blocks found on this page. Check it's the right page and that the user has permission.", 0)

		val matchingModule = moduleInfoBlocks.filter(_.underlying.findElement(By.className("mod-code")).getText == moduleCode.toUpperCase)

		if (matchingModule.isEmpty)
			throw new TestFailedException(s"No module-info found for ${moduleCode.toUpperCase}", 0)

		matchingModule.next().underlying
	}
}
