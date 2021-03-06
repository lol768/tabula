package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.{AssessmentService, ExtensionService}
import uk.ac.warwick.tabula.system.TwoWayConverter

class ExtensionConverter extends TwoWayConverter[String, Extension] {

	@Autowired var service: ExtensionService = _

	override def convertRight(id: String): Extension = (Option(id) flatMap { service.getExtensionById }).orNull
	override def convertLeft(extension: Extension): String = (Option(extension) map {_.id}).orNull

}