<#escape x as x?html>

<h1>Edit student relationship type: ${relationshipType.description}</h1>

<@f.form method="post" action="${url('/sysadmin/relationships/${relationshipType.urlPart}/edit')}" commandName="editStudentRelationshipTypeCommand" cssClass="form-horizontal">

	<@f.errors cssClass="error form-errors" />
	
	<#assign newRecord=false />
	<#include "_fields.ftl" />
	
	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn" href="<@url page="/sysadmin/relationships" />">Cancel</a>
	</div>

</@f.form>

<#if relationshipType.empty>
	<p class="subtle">
		Did you create this relationship type in error? 
		You may <a href="<@url page="/sysadmin/relationships/${relationshipType.urlPart}/delete" />" class="btn btn-danger">delete</a> it if you definitely won't need it again.
	</p>
<#else>
	<p class="subtle">
		It's not possible to delete this relationship type because there are relationships with this type.
	</p>
</#if>

</#escape>