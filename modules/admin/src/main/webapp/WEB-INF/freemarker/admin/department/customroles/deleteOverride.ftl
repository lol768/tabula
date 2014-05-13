<#assign form_url><@routes.deletecustomroleoverride roleOverride /></#assign>
<#escape x as x?html>
	<#compress>

	<p>Are you sure you want to delete this override?</p>

		<@f.form method="post" action="${form_url}" commandName="command" cssClass="form-horizontal">
			<@f.errors cssClass="error form-errors" />

			<input type="submit" class="btn btn-danger" value="Delete" />

			<a class="btn" href="<@routes.customroleoverrides customRoleDefinition />">Cancel</a>

		</@f.form>

	</#compress>
</#escape>