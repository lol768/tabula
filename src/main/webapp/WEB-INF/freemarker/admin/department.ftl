<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
 
<#macro longDateRange start end>
	<#assign openTZ><@warwick.formatDate value=start pattern="z" /></#assign>
	<#assign closeTZ><@warwick.formatDate value=end pattern="z" /></#assign>
	<@fmt.date start /> 
	<#if openTZ != closeTZ>(${openTZ})</#if>
	-<br>
	<@fmt.date end /> (${closeTZ})
</#macro>
 
<#if department??>
<h1>${department.name}</h1>

<#list modules as module>
<#assign can_manage=can.manage(module) />
<#assign has_assignments=(module.assignments!?size gt 0) />
<a id="module-${module.code}"></a>
<div class="module-info<#if !has_assignments> empty</#if>">
<h2><@fmt.module_name module /></h2>
	<div class="module-info-contents">
	
	<div>
		
		<#assign  module_managers = ((module.participants.includeUsers)![]) />
		<@fmt.p module_managers?size "module manager"/><#if module_managers?size gt 0>:
			<@fmt.user_list_csv ids=module_managers />
		</#if>
		<#if can_manage >	
		
		<a class="btn btn-mini" title="Edit module permissions" href="<@url page="/admin/module/${module.code}/permissions" />">
		Edit
		</a>
		
		</#if>
	</div>
	
	<#if !has_assignments >
		<p>This module has no assignments. 
		<span class="btn-group">
		<a class="btn" href="<@url page="/admin/module/${module.code}/assignments/new" />"><i class="icon-plus"></i> New assignment</a>
		</span>
		</p>
	<#else>
		<#list module.assignments as assignment>
		<#if !assignment.deleted>
		<#assign has_feedback = assignment.feedbacks?size gt 0 >
		<div class="assignment-info">
			<div class="column1">
			<h3 class="name">${assignment.name}</h3>
			<#if assignment.closed>
				<div><span class="label-orange">Closed</span></div>
			</#if>
			</div>
			<div class="stats">
				<div class="open-date">
					<span class="label-like"><@fmt.tense assignment.openDate "Opens" "Opened" /></span> <@fmt.date assignment.openDate /> 
				</div>
				<div class="close-date">
					<span class="label-like"><@fmt.tense assignment.closeDate "Closes" "Closed" /></span> <@fmt.date assignment.closeDate /> 
				</div>
				<#if features.submissions && assignment.collectSubmissions>
					<div class="submission-count">
						<#if assignment.submissions?size gt 0>
							<a href="<@routes.assignmentsubmissions assignment=assignment />" title="View all submissions">
								${assignment.submissions?size} submissions
							</a>
						<#else>
							${assignment.submissions?size} submissions
						</#if>
					</div>
				</#if>
				<div class="feedback-count">
				<#if has_feedback><a class="list-feedback-link" href="<@routes.assignmentfeedbacks assignment=assignment  />"></#if>
				${assignment.feedbacks?size} feedback<#if has_feedback></a></#if>
				<#assign unreleasedFeedback=assignment.unreleasedFeedback />
				<#if unreleasedFeedback?size gt 0>
					<span class="has-unreleased-feedback">
					(${unreleasedFeedback?size} to publish)
					</span>
				<#elseif has_feedback>
					<span class="no-unreleased-feedback">
					(all published)
					</span>
				</#if>
				</div>
				
				<#if assignment.anyReleasedFeedback || features.submissions>
				<p class="feedback-published">
					<#assign urlforstudents><@url page="/module/${module.code}/${assignment.id}"/></#assign>
					<a class="copyable-url" rel="tooltip" href="${urlforstudents}" title="This is the link you can freely give out to students or publish on your module web page. Click to copy it to the clipboard and then paste it into an email or page.">
						URL for students
					</a>
				</p>
				</#if>
				
			</div>
			<div class="assignment-buttons">
				<a class="btn edit-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/edit" />">Edit details <i class="icon-edit"></i></a>
				<a class="btn feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" />">Add feedback <i class="icon-plus"></i></a>
				<#if assignment.collectMarks >
					<a class="btn" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/marks" />">Add marks <i class="icon-plus"></i></a>
				</#if>
				<#if has_feedback >
				<a class="btn list-feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/list" />">List feedback <i class="icon-list-alt"></i></a>
				<#if assignment.canPublishFeedback>
				  <#if assignment.closed>
				    <a class="btn" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/publish" />">Publish feedback <i class="icon-envelope"></i></a>
				  <#else>
				    <a class="btn disabled" href="#" title="You can only publish feedback after the close date.">Publish feedback <i class="icon-envelope"></i></a>
				  </#if>
				</#if>
				</#if>
			</div>
			<div class="end-assignment-info"></div>
		</div>
		</#if>
		</#list>
		
		<div class="btn-group">
		<a class="btn" href="<@url page="/admin/module/${module.code}/assignments/new" />"><i class="icon-plus"></i> New assignment</a>
		</div>
	</#if>
	
	</div>
	
</div>
</#list>

<#else>
<p>No department.</p>
</#if>

</#escape>