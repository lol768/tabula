package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Feedback}
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.system.TwoWayConverter

class MarkerFeedbackIdConverter extends TwoWayConverter[String, MarkerFeedback] {

	@Autowired var service: FeedbackDao = _

	override def convertRight(id: String) = service.getMarkerFeedback(id).orNull
	override def convertLeft(feedback: MarkerFeedback) = (Option(feedback) map {_.id}).orNull

}
