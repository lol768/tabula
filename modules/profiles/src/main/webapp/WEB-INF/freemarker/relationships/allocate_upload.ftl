<#escape x as x?html>

<@spring.bind path="command">
	<#assign hasFileErrors=status.errors.allErrors?size gt 0 />
</@spring.bind>

<#assign previewFormAction><@routes.relationship_allocate_preview department relationshipType /></#assign>

<#if hasFileErrors>

	<@spring.bind path="command">
		<div class="alert alert-block alert-error">
			<#list status.errors.allErrors as error>
				<@spring.message message=error />
			</#list>
		</div>

		<a class="btn" href="<@routes.relationship_allocate department relationshipType />">Cancel</a>
	</@spring.bind>

<#else>

	<#assign hasRows = validRows?has_content />

	<@f.form class="form-horizontal" method="post" action="${previewFormAction}" commandName="command">
		<div class="fix-area">
			<h1>Spreadsheet upload of ${relationshipType.description} changes</h1>
			<h4><span class="muted">for</span> ${department.name}</h4>

			<#if invalidRows?has_content>
				<h2>Invalid rows</h2>
				<h6>The following rows had errors which prevent them being uploaded</h6>

				<table class="table table-bordered table-condensed table-striped">
					<thead>
					<tr>
						<th>${relationshipType.studentRole?cap_first} ID</th>
						<th>${relationshipType.studentRole?cap_first} name</th>
						<th>${relationshipType.agentRole?cap_first} ID</th>
						<th>${relationshipType.agentRole?cap_first} name</th>
						<th>Error</th>
					</tr>
					</thead>
					<tbody>
						<#list invalidRows as row>
							<tr>
								<td class="error">${row.studentId!}</td>
								<td class="error">${row.studentName!}</td>
								<td class="error">${row.agentId!}</td>
								<td class="error">${row.agentName!}</td>
								<td class="error"><@spring.message code=row.error /></td>
							</tr>
						</#list>
					</tbody>
				</table>
			</#if>

			<#if hasRows>
				<h2>Valid rows</h2>

				<table class="table table-bordered table-condensed table-striped">
					<thead>
					<tr>
						<th>${relationshipType.studentRole?cap_first} ID</th>
						<th>${relationshipType.studentRole?cap_first} Name</th>
						<th>${relationshipType.agentRole?cap_first} ID</th>
						<th>${relationshipType.agentRole?cap_first} Name</th>
					</tr>
					</thead>
					<tbody>
						<#list validRows as row>
							<tr>
								<td>${row.studentId!}</td>
								<td>${row.studentName!}</td>
								<td>${row.agentId!}</td>
								<td>${row.agentName!}</td>
							</tr>
						</#list>
					</tbody>
				</table>
			<#elseif invalidRows?has_content>
				<h2>Valid rows</h2>

				<div class="alert alert-block alert-error">
					<i class="icon-warning-sign"></i> There were no valid rows in the spreadsheet. Please review your spreadsheet data.
				</div>
			<#else>
				<div class="alert alert-block alert-error">
					<h2>No information was found in the spreadsheet</h2>

					<p>In order for students to be allocated or unallocated from their ${relationshipType.agentRole}, there must be
						at least two columns in the spreadsheet. One must have a header of <strong>student_id</strong> and contain
						University card numbers for students, and the other must have a header of <strong>agent_id</strong> and contain
						University card numbers of each student's ${relationshipType.agentRole}.</p>
				</div>
			</#if>
			<div class="fix-footer">
				<#if hasRows>
					<@form.labelled_row "allocationType" "Choose allocation type">
						<@form.label clazz="radio" checkbox=true>
							<@f.radiobutton path="allocationType" value="${allocationTypes.Replace}" />
							Replace existing ${relationshipType.agentRole}s
							<@fmt.help_popover id="allocationType-replace" content="For any student with a ${relationshipType.agentRole} defined in the spreadsheet, remove any existing ${relationshipType.agentRole}s and add the new ${relationshipType.agentRole}" />
						</@form.label>
						<@form.label clazz="radio" checkbox=true>
							<@f.radiobutton path="allocationType" value="${allocationTypes.Add}" />
							Add additional ${relationshipType.agentRole}s
							<@fmt.help_popover id="allocationType-replace" content="For any student with a ${relationshipType.agentRole} defined in the spreadsheet, add the new ${relationshipType.agentRole}. Any existing ${relationshipType.agentRole}s will remain" />
						</@form.label>
					</@form.labelled_row>
				</#if>

				<div class="submit-buttons">
					<#if hasRows>
						<#list command.additions?keys as entity>
							<#list command.additions[entity] as student>
								<input type="hidden" name="additions[${entity}]" value="${student}" />
							</#list>
						</#list>
						<#list command.additionalEntities as entity>
							<input type="hidden" name="additionalEntities" value="${entity}" />
						</#list>
						<button type="submit" class="btn btn-primary">Continue</button>
					</#if>

					<a class="btn" href="<@routes.relationship_allocate department relationshipType />">Cancel</a>
				</div>
			</div>
		</div>
	</@f.form>
</#if>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
	});
</script>


</#escape>