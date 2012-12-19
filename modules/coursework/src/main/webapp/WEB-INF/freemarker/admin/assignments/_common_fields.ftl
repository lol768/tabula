<#-- 

This section contains the form fields that can apply to a group of
assignments, as well as to an individual one.

If you add a field it should also be added to _common_fields_hidden.ftl
so that they can be passed around between requests.

-->

<#if features.feedbackTemplates && department.feedbackTemplates?has_content>
	<@form.labelled_row "feedbackTemplate" "Feedback template">
		<@f.select path="feedbackTemplate">
			<@f.option value="" label="No template"/>
			<#list department.feedbackTemplates as template>
				<@f.option value="${template.id}" label="${template.name}"/>
			</#list>
		</@f.select>
		<div class="help-block">
			Select the feedback template that will be used for this assignment. Copies of the template will be
			distributed along with student submissions.
		</div>
	</@form.labelled_row>
</#if>

<#if features.markSchemes && department.markSchemes?has_content>

	<#assign disabled = !(canUpdateMarkScheme!true)>

	<@form.labelled_row "markScheme" "Mark scheme">
		<@f.select path="markScheme" disabled="${disabled?string}">
			<@f.option value="" label="None"/>
			<#list department.markSchemes as markScheme>
				<@f.option value="${markScheme.id}" label="${markScheme.name}"/>
			</#list>
		</@f.select>
		<div class="help-block">
			<#if disabled>
				<span class="warning">You cannot change the mark scheme for this assignment as it already has submissions.</span>
			<#else>
				Select the way in which this assignment will be marked.
			</#if>
		</div>
	</@form.labelled_row>
</#if>

<#if features.submissions>
	<@form.labelled_row "collectSubmissions" "Submissions">
		<label class="checkbox">
			<@f.checkbox path="collectSubmissions" id="collectSubmissions" />
			Collect submissions
		</label>
	</@form.labelled_row>
	<fieldset id="submission-options">
		<legend>Submission options</legend>

		<#if features.collectMarks>
			<@form.row>
				<@form.label></@form.label>
				<@form.field>
					<label class="checkbox">
						<@f.checkbox path="collectMarks" />
						Collect marks
					</label>
				</@form.field>
			</@form.row>
		</#if>

		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="displayPlagiarismNotice" />
					Show plagiarism notice
				</label>
			</@form.field>
		</@form.row>

		<#if features.assignmentMembership>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="restrictSubmissions" />
					Only allow enrolled students to submit
				</label>
				<div class="help-block">
					If you use this option, only students defined above as members will be able to
					submit, so make sure that the membership is correct to avoid problems.
				</div>
			</@form.field>
		</#if>

		<@form.row cssClass="has-close-date">
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="allowLateSubmissions" />
					Allow new submissions after the close date
				</label>
			</@form.field>
		</@form.row>
		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="allowResubmission" />
					Allow students to re-submit work
				</label>
				<div class="help-block">
					Students will be able to submit new work, replacing any previous submission.
					Re-submission is <em>never</em> allowed after the close date.
				</div>
			</@form.field>
		</@form.row>

		<#if features.extensions>
			<@form.row cssClass="has-close-date">
				<@form.label></@form.label>
				<@form.field>
					<label class="checkbox">
						<@f.checkbox path="allowExtensions" id="allowExtensions" />
						Allow extensions
					</label>
				</@form.field>
			</@form.row>
			<!--div id="request-extension-row">
				<@form.row>
					<@form.label></@form.label>
					<@form.field>
						<label class="checkbox">
							<@f.checkbox path="allowExtensionRequests" />
							Allow students to request extensions
						</label>
						<div class="help-block">
							Students will be able to request extensions for this assignment via the submission page.
						</div>
					</@form.field>
				</@form.row>
			</div-->
		</#if>

		<@form.row>
			<@form.label path="fileAttachmentLimit">Max attachments per submission</@form.label>
			<@form.field>
				<@spring.bind path="maxFileAttachments">
					<#assign maxFileAttachments=status.actualValue />
				</@spring.bind>
				<@f.select path="fileAttachmentLimit" cssClass="span1">
					<@f.options items=1..maxFileAttachments />
				</@f.select>
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label path="fileAttachmentTypes">Accepted attachment file types</@form.label>
			<@form.field>
				<@f.errors path="fileAttachmentTypes" cssClass="error" />
				<@f.input path="fileAttachmentTypes"  type="hidden" />
				<script type="text/javascript" src="/static/js/textList.js"></script>
				<script type="text/javascript">
					jQuery(document).ready(function(){
						var textListController = new TextListController('#fileExtensionList', '#fileAttachmentTypes');
						textListController.transformInput = function(text){
							var result = text.replace(new RegExp('\\.', 'g') , '');
							return result.toLowerCase();
						};
						textListController.preventDuplicates = true;
						textListController.init();
					});
				</script>
				<div id="fileExtensionList" class="textBoxListContainer">
					<ul>
						<li class="inputContainer"><input class="text" type="text"></li>
					</ul>
				</div>
				<div class="help-block">
					Enter the file types you would like to allow separated by spaces (e.g. "pdf doc docx"). Only attachments with the extensions specified will be permitted. Leave this field blank to accept attachments with any extension.
				</div>
			</@form.field>
		</@form.row>

		<@form.row path="comment">
		  <@form.label for="assignmentComment">Text to show on submission form</@form.label>
		  	<@form.field>
				<@f.errors path="comment" cssClass="error" />
				<@f.textarea path="comment" id="assignmentComment" rows="6" cssClass="span6" />
				<div class="help-block">
					You can make a new paragraph by leaving a blank line (i.e. press Enter twice).
				</div>
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label path="wordCountMin">Minimum word count</@form.label>
			<@form.field>
				<@f.errors path="wordCountMin" cssClass="error" />
				<@f.input path="wordCountMin" cssClass="input-small" maxlength="${maxWordCount?c?length}" />
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label path="wordCountMax">Maximum word count</@form.label>
			<@form.field>
				<@f.errors path="wordCountMax" cssClass="error" />
				<@f.input path="wordCountMax" cssClass="input-small" maxlength="${maxWordCount?c?length}" />
				<div class="help-block">
					If you enter a minimum and/or maximum word count, students will be required to declare the word count for
					their submissions. They will not be allowed to submit unless their declaration is within your specified range.
					Students won't be asked if both boxes are left blank. There's a system-wide maximum of <@fmt.p maxWordCount "word" />.
				</div>
			</@form.field>
		</@form.row>
		
		<@form.row>
		  <@form.label for="wordCountConventions">Word count conventions</@form.label>
		  	<@form.field>
				<@f.errors path="wordCountConventions" cssClass="error" />
				<@f.textarea path="wordCountConventions" id="wordCountConventions" rows="3" cssClass="span6" />
				<div class="help-block">
					Tell students if there are specific things which should be included or excluded from the word count.
				</div>
			</@form.field>
		</@form.row>

	</fieldset>


<#--
	<@form.row>
	<@form.field>



	</@form.field>
	</@form.row>
-->

</#if>