package uk.ac.warwick.tabula.coursework.commands.feedback

import org.apache.poi.ss.usermodel.{ComparisonOperator, IndexedColors}
import org.apache.poi.ss.util.{CellRangeAddress, WorkbookUtil}
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assessment, Assignment, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFeedbackServiceComponent, FeedbackServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object GenerateOwnMarksTemplateCommand {
	def apply(module: Module, assignment: Assignment, members: Seq[String]) =
		new GenerateMarksTemplateCommandInternal(module, assignment, members)
			with AutowiringFeedbackServiceComponent
			with ComposableCommand[XSSFWorkbook]
			with GenerateOwnMarksTemplatePermissions
			with GenerateMarksTemplateCommandState
			with Unaudited with ReadOnly
}

object GenerateMarksTemplateCommand {
	def apply(module: Module, assignment: Assignment, members: Seq[String]) =
		new GenerateMarksTemplateCommandInternal(module, assignment, members)
			with AutowiringFeedbackServiceComponent
			with ComposableCommand[XSSFWorkbook]
			with GenerateAllMarksTemplatePermissions
			with GenerateMarksTemplateCommandState
			with Unaudited with ReadOnly
}

object MarksTemplateCommand {

	// util to replace unsafe characters with spaces
	def safeAssessmentName(assessment: Assessment) = WorkbookUtil.createSafeSheetName(trimmedAssessmentName(assessment))

	val MaxSpreadsheetNameLength = 31
	val MaxAssignmentNameLength = MaxSpreadsheetNameLength - "Marks for ".length

	// trim the assignment name down to 21 characters. Excel sheet names must be 31 chars or less so
	// "Marks for " = 10 chars + assignment name (max 21) = 31
	def trimmedAssessmentName(assessment: Assessment) = {
		if (assessment.name.length > MaxAssignmentNameLength)
			assessment.name.substring(0, MaxAssignmentNameLength)
		else
			assessment.name
	}

}

class GenerateMarksTemplateCommandInternal(val module: Module, val assignment: Assignment, val members: Seq[String]) extends CommandInternal[XSSFWorkbook] {

	self: FeedbackServiceComponent =>

	override def applyInternal() = {

		val workbook = new XSSFWorkbook()
		val sheet = generateNewMarkSheet(assignment, workbook)

		// populate the mark sheet with ids
		for ((member, i) <- members.zipWithIndex) {
			val row = sheet.createRow(i + 1)
			row.createCell(0).setCellValue(member)
			val marksCell = row.createCell(1)
			val gradesCell = row.createCell(2)
			val feedbacks = feedbackService.getStudentFeedback(assignment, member)
			feedbacks.foreach { feedback =>
				feedback.actualMark.foreach(marksCell.setCellValue(_))
				feedback.actualGrade.foreach(gradesCell.setCellValue)
			}
		}

		// add conditional formatting for invalid marks
		addConditionalFormatting(sheet)

		workbook
	}

	private def generateNewMarkSheet(assignment: Assignment, workbook: XSSFWorkbook) = {
		val sheet = workbook.createSheet("Marks for " + MarksTemplateCommand.safeAssessmentName(assignment))

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("ID")
		header.createCell(1).setCellValue("Mark")
		// TODO could perhaps have it calculated based on the grade boundaries
		header.createCell(2).setCellValue("Grade")

		sheet
	}

	private def addConditionalFormatting(sheet: XSSFSheet) = {
		val sheetCF = sheet.getSheetConditionalFormatting

		val invalidMarkRule = sheetCF.createConditionalFormattingRule(ComparisonOperator.NOT_BETWEEN, "0", "100")
		val fontFmt = invalidMarkRule.createFontFormatting
		fontFmt.setFontStyle(true, false)
		fontFmt.setFontColorIndex(IndexedColors.DARK_RED.index)

		val marksColumn = Array(new CellRangeAddress(1, sheet.getLastRowNum, 1, 1))
		sheetCF.addConditionalFormatting(marksColumn, invalidMarkRule)
	}

}

trait GenerateMarksTemplateCommandState {
	def module: Module
	def assignment: Assignment
}

trait GenerateOwnMarksTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateMarksTemplateCommandState =>

	mustBeLinked(assignment, module)

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.DownloadMarksTemplate, assignment)
	}

}

trait GenerateAllMarksTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateMarksTemplateCommandState =>

	mustBeLinked(assignment, module)

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.DownloadMarksTemplate, assignment)
	}

}
