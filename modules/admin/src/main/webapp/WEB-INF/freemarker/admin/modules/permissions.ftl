<#compress><#escape x as x?html>

<#import "/WEB-INF/freemarker/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/formatters.ftl" as fmt />
<#assign perms_url><@routes.moduleperms module /></#assign>
<#assign module_name><@fmt.module_name module /></#assign>

<div class="permissions-page">
	<div class="pull-right">
		<a class="btn" href="<@routes.permissions module />"><i class="icon-lock"></i> Advanced</a>
	</div>

	<h1 class="with-settings">Module permissions</h1>
	<h5><span class="muted">for</span> <#noescape>${module_name}</#noescape></h5>

	<@pm.alerts "addCommand" module_name users role />

	<#assign scope=module />
	<#include "_roles.ftl" />
</div>

<@pm.script />

</#escape></#compress>