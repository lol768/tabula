package uk.ac.warwick.tabula.attendance.manage

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceCreateSchemeTest extends AttendanceFixture with GivenWhenThen {

	val schemeName = "The Scheme of things"

	"A Member of staff" should "be able to create monitoring point schemes" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		// HTMLUnit javascript messes up the DOM when you have use-tooltip on a form element you want to query for
		// Without disabling js, it's impossible to click on the submit button
		ifHtmlUnitDriver(h=>h.setJavascriptEnabled(false))

		When(s"I go to /attendance/manage/xxx/$thisAcademicYearString/new")
		go to Path(s"/attendance/manage/xxx/$thisAcademicYearString/new")

		And("I enter a scheme name")
		click on id("name")
		pressKeys(schemeName)

		And("I select 'term weeks' as the Date format")
		radioButtonGroup("pointStyle").value= "week"

		Then("I create the scheme and add students")
		click on cssSelector("#main-content form input.btn.btn-success")
		eventually(currentUrl should endWith(s"students"))

		When("I choose a route")
		click on cssSelector("#main-content input[name=routes]")

		And("I click on Find")
		click on cssSelector("#main-content button[name=findStudents]")

		Then("I see the students")
		eventually(
			findAll(cssSelector("details.find-students table.manage-student-table tbody tr")).size should be (2)
		)
		pageSource should include("2 students on this scheme")
		pageSource should include("(2 from SITS)")

		When("I add a student manually")
		click on cssSelector("input[name=manuallyAddForm]")
		eventually(pageSource should include("Add students manually"))
		click on cssSelector("textarea[name=massAddUsers]")
		pressKeys("tabula-functest-student2")
		click on cssSelector("#main-content form input.btn.btn-success")

		Then("I see the manually added student")
		eventually(
			findAll(cssSelector("details.manually-added table.manage-student-table tbody tr")).size should be (1)
		)
		pageSource should include("3 students on this scheme")
		pageSource should include("(2 from SITS, plus 1 added manually)")

		When("I save the scheme")
		click on cssSelector("#main-content form input.btn.btn-primary")

		Then("I am redirected to the manage home page")
		eventually(currentUrl should endWith(s"/attendance/manage/xxx/$thisAcademicYearString"))
		pageSource should include(s"Manage monitoring points for ${FunctionalTestAcademicYear.current.toString}")
		pageSource should include(schemeName)

		When("The I click the 'Add points' link")
		click on linkText("Add points")

		Then("I am redirected to the add points page")
		eventually(currentUrl should include(s"/attendance/manage/xxx/$thisAcademicYearString/addpoints"))
		pageSource should include(schemeName)

	}
}
