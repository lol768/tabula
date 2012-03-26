<#compress>
<#escape x as x?html>

<#if !embedded>
<h1>Submissions/feedback comparison for ${assignment.name}</h1>
</#if>

<#if report.hasProblems>

	<#if submissionOnly?size gt 0>
	<div class="potential-problem">
		<p>These users have submitted to the assignment, but no feedback has been uploaded for them.</p>
		
		<ul class="user-list">
		<#list submissionOnly as u>
			<li>${u.warwickId}</li>
		</#list>
		</ul>
	</div>
	</#if>
	
	<#if feedbackOnly?size gt 0>
	<div class="potential-problem">
		<p>There is feedback for these users but they did not submit an assignment to this system.</p>
		
		<ul class="user-list">
		<#list feedbackOnly as u>
			<li>${u.warwickId}</li>
		</#list>
		</ul>
	</div>
	</#if>
	
	
	<p>
		The above discrepencies are provided for information.
		It is up to you to decide whether to continue publishing. 
	</p>
	

<#else>
	<p><span class="icon-ok"></span> The submissions and the feedback items appear to match up for this assignment.</p>
</#if>

</#escape>
</#compress>