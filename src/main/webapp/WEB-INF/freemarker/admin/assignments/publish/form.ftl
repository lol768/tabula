<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign module=assignment.module />
<#assign department=module.department />

<script type="text/javascript">
jQuery(function($){ "use strict";
	var submitButton = $('#publish-submit'),
		checkbox = $('#confirmCheck');
	function updateCheckbox() {
	  submitButton.attr('disabled', !checkbox.is(':checked'));
	}
	checkbox.change(updateCheckbox);
	updateCheckbox();
	
	$('#feedback-check-recipient-results')
		.html('<p>Checking for potential problems with students\' email addresses&hellip;</p>')
		.load('/admin/module/${module.code}/assignments/${assignment.id}/check-recipients');
		
	$('#submissions-report-results')
		.html('<p>Comparing feedback list against submission list&hellip;</p>')
		.load('/admin/module/${module.code}/assignments/${assignment.id}/submissions-report');
});
</script>

<@f.form method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/publish" commandName="publishFeedbackCommand">

<h1>Publish feedback for ${assignment.name}</h1>

<@f.errors path="assignment" cssClass="error" />

<#assign feedbackCount=assignment.feedbacks?size />
<#assign unreleasedFeedbackCount=assignment.unreleasedFeedback?size />

<p>This will publish feedback for ${unreleasedFeedbackCount} students.
<#if feedbackCount != unreleasedFeedbackCount>
There are ${feedbackCount} students in total but some have already had 
their feedback published to them and those students won't be emailed again.
</#if>
</p>

<p>
Publishing feedback will make feedback available for students to download. It can only be
done once for an assignment, and cannot be undone. Be sure that you have received all the
feedback you need before publishing, and then check the box below.
</p>

<#if features.emailStudents>
<p>
Each student will receive an email containing the link to the feedback. They will sign in
and be shown the feedback specific to them.
</p>
<#else>
<p>
Note: notifications are not currently send to students - you will need to distribute the
link yourself, by email or by posting it on your module web pages.
</p>
</#if>

<div id="feedback-check-recipient-results"></div>
<#if features.submissions && assignment.submissions?size gt 0>
<div id="submissions-report-results"></div>
</#if>

<@f.errors path="confirm" cssClass="error" />
<@f.checkbox path="confirm" id="confirmCheck" />
<@f.label for="confirmCheck"><strong> I have read the above and am ready to release feedback to students.</strong></@f.label>

<div class="submit-buttons">
<input type="submit" id="publish-submit" value="Publish">
</div>
</@f.form>

</#escape>