<#escape x as x?html>

<#assign showMyStudents = smallGroups?has_content />
<#list relationshipTypesMap?values as has_relationship>
	<#assign showMyStudents = showMyStudents || has_relationship />
</#list>

<#if !user.loggedIn> <#-- Defensive: For if we ever decide not to force login for /profiles/ -->
	<p class="lead muted">
		This is a service for managing student profiles, records and tutor information
	</p>

	<#if IS_SSO_PROTECTED!true>
		<p class="alert">
			You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
			to see a personalised view.
		</p>
	</#if>
<#else>
	<#assign is_staff=searchProfilesCommand?has_content />
	<#assign is_tutor=showMyStudents />
	<#assign is_admin=adminDepartments?has_content />

	<p class="lead muted">
		This is a service for managing student profiles, records and tutor information
	</p>

	<div class="row-fluid">
		<div class="span<#if is_admin>6<#else>12</#if>">
			<#if is_staff>
				<div class="header-with-tooltip" id="search-header">
					<h2 class="section">Search for students</h2>
					<span class="use-tooltip" data-toggle="tooltip" data-html="true" data-placement="bottom" data-title="Start typing a student's name, or put their University ID in, and we'll show you a list of results. Any student who studies in your department should be included.">Which students can I search for?</span>
				</div>

				<#include "../profile/search/form.ftl" />

				<#if universityId?has_content>
					<h2><a href="<@routes.profiles.profile_by_id universityId />">My staff profile</a></h2>
				</#if>
			</#if>

			<#if isPGR>
				<#if universityId?has_content>
					<h2><a href="<@routes.profiles.profile_by_id universityId />">My student profile</a></h2>
				</#if>
			</#if>

			<#if showMyStudents>
				<h2>My students</h2>

				<ul>
					<#list relationshipTypesMap?keys as relationshipType>
						<#if relationshipTypesMapById[relationshipType.id]>
							<li><a href="<@routes.profiles.relationship_students relationshipType />">${relationshipType.studentRole?cap_first}s</a></li>
						</#if>
					</#list>

					<#list smallGroups as smallGroup>
						<#assign _groupSet=smallGroup.groupSet />
						<#assign _module=smallGroup.groupSet.module />
						<li><a href="<@routes.profiles.smallgroup smallGroup />">
							${_module.code?upper_case} (${_module.name}) ${_groupSet.nameWithoutModulePrefix}, ${smallGroup.name}
						</a></li>
					</#list>
				</ul>
			<#elseif is_staff>
				<h2>My students</h2>

				<p>
					You are not currently the tutor for any group of students in Tabula. If you think this is incorrect, please contact your
					departmental access manager for Tabula, or email <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
				</p>
			</#if>

			<#if searchDepartments??>
				<#list searchDepartments as dept>
					<h2>${dept.name}</h2>
					<ul>
						<li><a href="<@routes.profiles.filter_students dept />">All students in ${dept.name}</a></li>
						<li><a href="<@routes.profiles.department_timetables dept />">Timetables for ${dept.name}</a></li>
					</ul>
				</#list>
			</#if>
		</div>

		<#if adminDepartments?has_content>
			<div id="profile-dept-admin" class="span6">
				<h4>Departmental administration</h4>

				<#list adminDepartments as dept>
				<div class="clearfix">
					<div class="btn-group pull-right">
					  <a class="btn btn-small dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
					  <ul class="dropdown-menu pull-right">
							<li><a href="<@routes.profiles.deptperms dept/>">
								<i class="icon-fixed-width icon-user"></i> Edit departmental permissions
							</a></li>

							<li><a href="<@routes.profiles.filter_students dept/>">
								<i class="icon-fixed-width icon-group"></i> View students
							</a></li>

							<#list dept.displayedStudentRelationshipTypes as relationshipType>
								<li><a href="<@routes.profiles.relationship_agents dept relationshipType />">
									<i class="icon-fixed-width icon-eye-open"></i> ${relationshipType.description}s
								</a></li>
								<li><a href="<@routes.profiles.relationship_missing dept relationshipType />">
									<i class="icon-fixed-width icon-eye-close"></i> Students with no ${relationshipType.description}
								</a></li>

								<#if features.personalTutorAssignment && !relationshipType.readOnly(dept)>
									<li><a href="<@routes.profiles.relationship_allocate dept relationshipType />">
										<i class="icon-random icon-fixed-width"></i> Allocate ${relationshipType.description}s</a>
									</li>
								</#if>
							</#list>

							<li><a href="<@routes.profiles.displaysettings dept />?returnTo=${(info.requestedUri!"")?url}">
								<i class="icon-fixed-width icon-list-alt"></i> Settings</a>
							</li>
					  </ul>
					</div>

					<h5>${dept.name}</h5>
				</div>

				<#if dept_has_next><hr></#if>
				</#list>
			</div>
		</#if>
	</div>
</#if>
</#escape>