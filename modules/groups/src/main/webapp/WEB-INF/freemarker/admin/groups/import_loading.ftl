<#escape x as x?html>
	<#macro deptheaderroutemacro department>
		<@routes.import_groups_for_year department academicYear />
	</#macro>
	<#assign deptheaderroute = deptheaderroutemacro in routes />

	<@fmt.deptheader "Import small groups from Syllabus+" "for" department routes "deptheaderroute" "" />

	<#assign post_url><@routes.import_groups department /></#assign>
	<@f.form method="post" id="import-form" action="${post_url}" commandName="command" cssClass="form-horizontal">
		<input type="hidden" name="action" value="populate" />
		<@f.hidden path="academicYear" />
	</@f.form>

	<div class="muted">Loading, please wait&hellip;</div>

	<script type="text/javascript">
		jQuery(function($) {
			$('#import-form').submit();
		});
	</script>
</#escape>