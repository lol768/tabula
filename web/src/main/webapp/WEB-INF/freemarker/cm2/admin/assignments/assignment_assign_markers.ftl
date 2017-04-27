<#escape x as x?html>
<#import "*/assignment_components.ftl" as components />
<#include "assign_marker_macros.ftl" />

<div class="deptheader">
	<h1>Create a new assignment</h1>
	<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area">
	<#assign actionUrl><@routes.cm2.createassignmentmarkers assignment /></#assign>
	<@f.form method="post" action=actionUrl  cssClass="dirty-check">
		<@components.assignment_wizard 'markers' assignment.module false assignment />

		<p class="btn-toolbar">
			<a class="return-items btn btn-default" href="<@routes.cm2.createassignmentmarkerstemplate assignment/>" >
				Upload spreadsheet
			</a>
			<a class="return-items btn btn-default" href="#" >
				Import small groups
			</a>
		</p>

		<@f.errors cssClass="error form-errors" />
		<#list state.keys as role>
			<@allocateStudents assignment role mapGet(stages, role) mapGet(state.markers, role) mapGet(state.unallocatedStudents, role) mapGet(state.allocations, role)  />
		</#list>
		<div class="fix-footer">
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddSubmissions}"
					value="Save and continue"
			/>
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddMarkers}"
					value="Save and exit"
			/>
		</div>
	</@f.form>
</div>
</#escape>