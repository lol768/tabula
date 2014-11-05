<#escape x as x?html>
<#assign formAction><@routes.markingUncompleted assignment /></#assign>

<@f.form method="post" action="${formAction}" commandName="markingUncompletedCommand">

<h1>Marking uncompleted</h1>

<@form.errors path="" />

<input type="hidden" name="onlineMarking" value="${onlineMarking?string}" />
<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="students">
	<@form.errors path="students" />
	<#assign students = status.actualValue />

	<p>
		<strong><@fmt.p markingUncompletedCommand.completedMarkerFeedback?size "student" /></strong> submissions will be no longer listed as completed.
	</p>

	<#list students as uniId>
		<input type="hidden" name="students" value="${uniId}" />
	</#list>
</@spring.bind>
<p>
	<@form.errors path="confirm" />
	<@form.label checkbox=true><@f.checkbox path="confirm" />
		<#if (students?size > 1)>
			I confirm that I would like to undo marking completion for these student's submissions.
		<#else>
			I confirm that I would like to undo marking completion for this student's submission.
		</#if>
	</@form.label>
</p>

<div class="submit-buttons">
	<input class="btn btn-warn" type="submit" value="Confirm">
	<a class="btn" href="<@routes.listmarkersubmissions assignment />">Cancel</a>
</div>
</@f.form>
</#escape>