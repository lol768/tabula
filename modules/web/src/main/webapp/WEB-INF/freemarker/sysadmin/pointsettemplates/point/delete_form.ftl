<#escape x as x?html>

<div class="modal-header">
	<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	<h2>Delete monitoring point</h2>
</div>

<div class="modal-body">

	<#assign action><@url page="/sysadmin/pointsettemplates/add/points/delete/${command.pointIndex}" /></#assign>

	<@f.form id="deleteMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<#list command.monitoringPoints as point>
			<input type="hidden" name="monitoringPoints[${point_index}].name" value="${point.name}" />
            <input type="hidden" name="monitoringPoints[${point_index}].validFromWeek" value="${point.validFromWeek}" />
            <input type="hidden" name="monitoringPoints[${point_index}].requiredFromWeek" value="${point.requiredFromWeek}" />
		</#list>
		<#assign point = command.monitoringPoints[pointIndex] />
		<p>You are deleting the monitoring point: ${point.name} (week ${point.validFromWeek} - ${point.requiredFromWeek}).</p>

		<p>
			<@form.label checkbox=true>
				<@f.checkbox path="confirm" /> I confirm that I want to delete this monitoring point.
			</@form.label>
			<@form.errors path="confirm"/>
		</p>

	</@f.form>

</div>
<div class="modal-footer">
	<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Deleting&hellip;">
		Delete
	</button>
	<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
</div>


</#escape>