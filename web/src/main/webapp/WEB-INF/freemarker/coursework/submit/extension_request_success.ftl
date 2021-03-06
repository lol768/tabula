<#escape x as x?html>
<#compress>
	<h1>You have requested an extension</h1>
	<h4><span class="muted">for</span> ${assignment.name}</h4>

	<p>
		You will receive an email when your request has been reviewed. If your request for an extension has not been
		approved before the deadline, hand in any work that you have completed before the deadline passes.
	</p>
	<p>
		If your circumstances change and you wish to provide additional information then you can edit your request by
		revisiting the submission page and pressing the button to review your request.
	</p>
	<a href="<@routes.coursework.assignment assignment=assignment />">Back to ${assignment.name}</a>
</#compress>
</#escape>