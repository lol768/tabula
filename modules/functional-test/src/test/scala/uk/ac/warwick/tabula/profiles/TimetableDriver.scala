package uk.ac.warwick.tabula.profiles

import scala.xml.Elem
import uk.ac.warwick.tabula.home.FixturesDriver
import uk.ac.warwick.tabula.FunctionalTestProperties
import dispatch.classic._

trait TimetableDriver extends FixturesDriver{

	def setTimetableFor(userId:String, content:Elem) {
		val uri = FunctionalTestProperties.SiteRoot + "/scheduling/stubTimetable/student"
		val req = url(uri).POST << Map("studentId" -> userId, "content"->content.toString)
		http.when(_==200)(req >| )
	}
}
