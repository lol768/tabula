<#escape x as x?html>

<#assign submitUrl><@routes.enrolment smallGroupSet /></#assign>
<@f.form method="post" action="${submitUrl}" commandName="command" cssClass="form-horizontal">
	<@f.errors cssClass="error form-errors" />

	<#import "*/membership_picker_macros.ftl" as membership_picker />
	<@membership_picker.header command />
	<@membership_picker.fieldset command 'group' 'group set' submitUrl />
</@f.form>
</#escape>