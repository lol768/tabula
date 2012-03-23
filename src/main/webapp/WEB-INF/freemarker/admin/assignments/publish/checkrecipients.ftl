<#compress>
<#escape x as x?html>

<#if report.hasProblems>

	<#assign problems = report.problems />
	<div class="bad-recipients potential-problem">
	<p>
		<@fmt.p problems?size "problem" /> found with students' and/or their email addresses.
		You can continue to publish the feedback, but you may want to post the feedback link to
		a web page that the students can access even if they didn't get the email. You'll be given
		this link after you publish.
	</p>
	<ul>
	<#list problems as problem>
		<li>
			<b>${problem.universityId}</b> :
			<#if problem.user?? >
				Bad email address: (problem.user.email)
			<#else>
				No such user found.
			</#if>
		</li>
	</#list>
	</ul>
	</div>
<#else>
	<p>No problems found with students' email addresses.</p>
</#if>

</#escape>
</#compress>