package uk.ac.warwick.tabula.attendance

import org.scalatest.GivenWhenThen

class AttendanceViewAgentsPageTest extends AttendanceFixture with GivenWhenThen{

	"A department admin" should "see the View Personal Tutors page" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		And("Marker 1 is tutor to Student 1")
		createStudentRelationship(P.Student1,P.Marker1)

		When("I go to /attendance/view/xxx/agents/tutor")
		go to Path("/attendance/view/xxx/agents/tutor")

		Then("I see the list of tutors")
		pageSource should include("Personal Tutors")
		pageSource should include("Personal Tutees")
		pageSource should include(P.Marker1.usercode)

		And("There is an Attendance button")
		click on cssSelector("table.agents td.button a")

		eventually(currentUrl should include(s"/attendance/view/xxx/agents/tutor/${P.Marker1.warwickId}"))
	}

}
