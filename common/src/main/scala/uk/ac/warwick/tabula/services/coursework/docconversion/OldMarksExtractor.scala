package uk.ac.warwick.tabula.services.coursework.docconversion

import uk.ac.warwick.spring.Wire

import scala.collection.JavaConverters._
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.springframework.stereotype.Service
import org.xml.sax.InputSource
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import java.io.InputStream

import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

class MarkItem {
	var universityId: String = _
	var user: User = _
	var actualMark: String = _
	var actualGrade: String = _
	var isValid = true
	var isModified = false
	var isPublished = false
	var hasAdjustment = false

	def studentIdentifier = Option(user.getWarwickId).getOrElse(user.getUserId)
}

@Service
class OldMarksExtractor extends AutowiringUserLookupComponent {

	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of MarkItems
	 */
	def readXSSFExcelFile(file: InputStream): JList[MarkItem] = {
		val pkg = OPCPackage.open(file)
		val sst = new ReadOnlySharedStringsTable(pkg)
		val reader = new XSSFReader(pkg)
		val styles = reader.getStylesTable
		val markItems: JList[MarkItem] = JArrayList()
		val sheetHandler = OldMarkItemXslxSheetHandler(styles, sst, markItems, userLookup)
		val parser = sheetHandler.fetchSheetParser
		for (sheet <- reader.getSheetsData.asScala) {
			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet.close()
		}
		markItems.asScala.filterNot(markItem => markItem.universityId == null && markItem.actualMark == null && markItem.actualGrade == null).asJava
	}
}

trait MarksExtractorComponent {
	val marksExtractor: OldMarksExtractor
}

trait AutowiringMarksExtractorComponent extends MarksExtractorComponent {
	val marksExtractor: OldMarksExtractor = Wire[OldMarksExtractor]
}
