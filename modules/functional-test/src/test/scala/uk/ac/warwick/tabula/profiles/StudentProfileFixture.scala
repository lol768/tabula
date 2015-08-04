package uk.ac.warwick.tabula.profiles

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.web.FixturesDriver

trait StudentProfileFixture extends FixturesDriver with GivenWhenThen {
	self: BrowserTest =>

	val TEST_ROUTE_CODE="xx123"
	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_COURSE_CODE="Ux123"

	before{
		Given("The test department exists")
		go to (Path("/scheduling/fixtures/setup"))

		And("student1 has a membership record")
		createRoute(TEST_ROUTE_CODE, TEST_DEPARTMENT_CODE, "Test Route")
		createCourse(TEST_COURSE_CODE, "Test Course")
		createStudentMember(P.Student1.usercode, routeCode=TEST_ROUTE_CODE, courseCode=TEST_COURSE_CODE, deptCode = TEST_DEPARTMENT_CODE)
	}
}