<#escape x as x?html>
<#import "*/cm2_macros.ftl" as cm2 />
<@cm2.assignmentHeader "Upload bulk adjustment" assignment "for" />
<p>
	Upload marks in a spreadsheet, which must be saved as an .xlsx file (ie created in Microsoft Office 2007 or later).
	The spreadsheet should have at least three column headings in the following order: <b>${StudentIdHeader}, ${MarkHeader}, ${GradeHeader}</b>.
	You can use this <a href="<@routes.cm2.feedbackBulkAdjustmentTemplate assignment />" >generated spreadsheet</a> as a template.
	Note that you can upload just marks, or both marks and grades.
</p>
	<#assign formUrl><@routes.cm2.feedbackBulkAdjustment assignment /></#assign>
	<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" commandName="command">
		<input name="isfile" value="true" type="hidden"/>
		<h3>Select file</h3>
		<@bs3form.labelled_form_group path="file.upload" labelText="File">
			<input type="file" name="file.upload" />
		</@bs3form.labelled_form_group>
		<@f.errors path="file" cssClass="error" />
		<div>
			<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
		</div>
	</@f.form>
</#escape>