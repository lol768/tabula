<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#compress>

<h1>Create module in ${department.name}</h1>
<#assign commandName="addModuleCommand" />
<#assign command=addModuleCommand />
<#assign submitUrl><@routes.admin.createmodule department /></#assign>
<@f.form method="post" action="${submitUrl}" commandName=commandName cssClass="form-horizontal">
<@f.errors cssClass="error form-errors" />

	<@f.hidden path="department" />

	<@form.labelled_row "code" "Module code">
		<@f.input path="code" cssClass="text" />
	</@form.labelled_row>

	<@form.labelled_row "name" "Module name">
		<@f.input path="name" cssClass="text" />
	</@form.labelled_row>

<div class="submit-buttons form-actions">
<input type="submit" value="Create" class="btn btn-primary">
<a class="btn" href="<@routes.admin.home />">Cancel</a>
</div>

</@f.form>

</#compress>
</#escape>