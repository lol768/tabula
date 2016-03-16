<#escape x as x?html>
	<#import "../attendance_variables.ftl" as attendance_variables />
	<#import "../attendance_macros.ftl" as attendance_macros />

<article class="profile">
	<section id="personal-details">
		<@fmt.member_photo student />
		<header>
			<h1><@fmt.profile_name student /></h1>
			<h5><@fmt.profile_description student /></h5>
		</header>

		<div class="data clearfix">
			<div class="col1">
				<table class="profile-info">
					<tbody>
						<tr>
							<th>Official name</th>
							<td>${student.officialName}</td>
						</tr>

						<tr>
							<th>Preferred name</th>
							<td>${student.fullName}</td>
						</tr>

						<#if student.gender??>
							<tr>
								<th>Gender</th>
								<td>${student.gender.description}</td>
							</tr>
						</#if>

						<#if student.nationality??>
							<tr>
								<th>Nationality</th>
								<td><@fmt.nationality student.nationality!('Unknown') /></td>
							</tr>
						</#if>

						<#if student.dateOfBirth??>
							<tr>
								<th>Date of birth</th>
								<td><@warwick.formatDate value=student.dateOfBirth.toDateTimeAtStartOfDay() pattern="dd/MM/yyyy" /></td>
							</tr>
						</#if>

						<#if student.student && student.termtimeAddress??>
							<tr class="address">
								<th>Term-time address</th>
								<td><@student_macros.address student.termtimeAddress /></td>
							</tr>
						</#if>

						<#if student.student && student.nextOfKins?? && student.nextOfKins?size gt 0>
							<tr>
								<th>Emergency contacts</th>
								<td>
									<#list student.nextOfKins as kin>
										<div>
											<#if kin.firstName?? && kin.lastName??>${kin.fullName}</#if>
											<#if kin.relationship??>(${kin.relationship})</#if>
										</div>
									</#list>
								</td>
							</tr>
						</#if>
					</tbody>
				</table>

				<br class="clearfix">
			</div>

			<div class="col2">
				<table class="profile-info">
					<tbody>
						<#if student.email??>
							<tr>
								<th>Warwick email</th>
								<td><i class="icon-envelope-alt"></i> <a href="mailto:${student.email}">${student.email}</a></td>
							</tr>
						</#if>

						<#if student.homeEmail??>
							<tr>
								<th>Alternative email</th>
								<td><i class="icon-envelope-alt"></i> <a href="mailto:${student.homeEmail}">${student.homeEmail}</a></td>
							</tr>
						</#if>

						<#if student.phoneNumber??>
							<tr>
								<th>Phone number</th>
								<td>${phoneNumberFormatter(student.phoneNumber)}</td>
							</tr>
						</#if>

						<#if student.mobileNumber??>
							<tr>
								<th>Mobile phone</th>
								<td>${phoneNumberFormatter(student.mobileNumber)}</td>
							</tr>
						</#if>

						<#if student.universityId??>
							<tr>
								<th>University number</th>
								<td>${student.universityId}</td>
							</tr>
						</#if>

						<#if student.userId??>
							<tr>
								<th>IT code</th>
								<td>${student.userId}</td>
							</tr>
						</#if>

						<#if student.student && student.homeAddress??>
							<tr class="address">
								<th>Home address</th>
								<td><@student_macros.address student.homeAddress /></td>
							</tr>
						</#if>
					</tbody>
				</table>
			</div>
		</div>

	</section>
</article>

<#if groupedPointMap?keys?size == 0>
	<p><em>No monitoring points found for this academic year.</em></p>
<#else>
	<#assign returnTo><@routes.attendance.agentStudent relationshipType academicYear.startYear?c student /></#assign>
	<a class="btn btn-primary" href="<@routes.attendance.agentRecord relationshipType academicYear.startYear?c student returnTo />">Record attendance</a>
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if groupedPointMap[term]??>
			<@attendance_macros.groupedPointsBySection groupedPointMap term; groupedPointPair>
				<#assign point = groupedPointPair._1() />
				<div class="span10">
					${point.name}
					(<a class="use-tooltip" data-html="true" title="
						<@fmt.wholeWeekDateFormat
							point.startWeek
							point.endWeek
							point.scheme.academicYear
						/>
					"><@fmt.monitoringPointWeeksFormat
						point.startWeek
						point.endWeek
						point.scheme.academicYear
						department
					/></a>)
				</div>
				<div class="span2">
					<#if groupedPointPair._2()??>
						<@attendance_macros.checkpointLabel department=department checkpoint=groupedPointPair._2() />
					<#else>
						<@attendance_macros.checkpointLabel department=department point=groupedPointPair._1() student=student />
					</#if>
				</div>
			</@attendance_macros.groupedPointsBySection>
		</#if>
	</#list>

	<#list monthNames as month>
		<#if groupedPointMap[month]??>
			<@attendance_macros.groupedPointsBySection groupedPointMap month; groupedPointPair>
				<#assign point = groupedPointPair._1() />
				<div class="span10">
					${point.name}
					(<@fmt.interval point.startDate point.endDate />)
				</div>
				<div class="span2">
					<#if groupedPointPair._2()??>
						<@attendance_macros.checkpointLabel department=department checkpoint=groupedPointPair._2() />
					<#else>
						<@attendance_macros.checkpointLabel department=department point=groupedPointPair._1() student=student />
					</#if>
				</div>
			</@attendance_macros.groupedPointsBySection>
		</#if>
	</#list>
</#if>

</#escape>