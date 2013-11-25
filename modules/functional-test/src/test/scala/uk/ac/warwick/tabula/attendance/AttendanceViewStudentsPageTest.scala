package uk.ac.warwick.tabula.attendance

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import org.openqa.selenium.By

class AttendanceViewStudentsPageTest extends AttendanceFixture with GivenWhenThen{

	"A Member of staff" should "see the View Students page" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When("I go to /attendance/view/xxx/students")
		go to Path("/attendance/view/xxx/students")

		Then("I see some students")
		pageSource should include("First name")
		className("scrollable-points-table").webElement.findElements(By.tagName("tr")).size() should be > 0
	}

}
