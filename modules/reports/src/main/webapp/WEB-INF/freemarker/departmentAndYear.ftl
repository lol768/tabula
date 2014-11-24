<#escape x as x?html>

<#macro deptheaderroutemacro dept>
	<@routes.departmentWithYear dept academicYear />
</#macro>
<#assign deptheaderroute = deptheaderroutemacro in routes/>
<@fmt.deptheader "View reports for ${academicYear.toString}" "in" department routes "deptheaderroute" />

<h2>Monitoring points</h2>

<ul>
	<li><h3><a href="<@routes.allAttendance department academicYear />">All attendance</a></h3></li>
	<li><h3><a href="<@routes.unrecordedAttendance department academicYear />">Unrecorded monitoring points</a></h3></li>
	<li><h3><a href="<@routes.missedAttendance department academicYear />">Missed monitoring points</a></h3></li>
</ul>

<h2>Small group teaching</h2>

<ul>
	<li>
		<h3>
			<a href="<@routes.allSmallGroups department academicYear />">All event attendance</a>
		</h3>
	</li>
</ul>

</#escape>