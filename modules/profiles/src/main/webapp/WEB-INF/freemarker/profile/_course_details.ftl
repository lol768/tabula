<section id="course-details" class="clearfix">
	<hr class="full-width"></hr>

	<!-- course heading -->
	<h3 class="course-title">
		Course: ${(studentCourseDetails.course.name)!}, ${(studentCourseDetails.course.code)!}
	<@fmt.course_year_span studentCourseDetails />

	</h3>

	<!-- drop-down to choose other courses: -->
<#if (profile.freshStudentCourseDetails)?? && (profile.freshStudentCourseDetails?size > 1)>
	<div class="dropdown">
		<a id="course-dropdown" class="dropdown-toggle pull-right" data-toggle="dropdown" role="button" href="#">
			Other courses
			<b class="caret"></b>
		</a>
		<ul class="dropdown-menu pull-right" aria-labelledby="course-dropdown" role="menu">
			<#list profile.freshStudentCourseDetails as scd>
				<#if scd.scjCode != studentCourseDetails.scjCode>
					<li role="presentation">
						<a href="/profiles/view/course/${scd.urlSafeId}" role="menuitem"
							<#if scd.scjCode == profile.mostSignificantCourse.scjCode>
						   id="most-significant"
							</#if>
								>
						<#-- when choosing from the "Other courses" drop-down, make the most signicant course
						  -- bold to assist with navigating back if you have previously gone into a different course -->
								${(scd.course.code)!}
								<@fmt.course_year_span studentCourseDetails /> ${scd.scjCode}
						</a>
					</li>
				</#if>
			</#list>
		</ul>
	</div>
</#if>


	<!-- basic course details across years -->
	<div class="data clearfix">
		<div class="col1 basic-course-details">
			<table class="profile-or-course-info">
				<tbody>
				<#if (studentCourseDetails.route)??>
				<tr>
					<th>Route</th>
					<td>${(studentCourseDetails.route.name)!} (${(studentCourseDetails.route.code?upper_case)!})
					</td>
				</tr>
				</#if>
				<#if (studentCourseDetails.department)??>
				<tr>
					<th>Department</th>
					<td>${(studentCourseDetails.department.name)!} (${((studentCourseDetails.department.code)!)?upper_case})
					</td>
				</tr>
				</#if>
				<#if !isSelf>
				<tr>
					<th>Status on Route</th>
					<td><@fmt.status_on_route studentCourseDetails />
					</td>
				</tr>
				</#if>
				<tr>
					<th>UG/PG</th>
					<td>${(studentCourseDetails.route.degreeType.toString)!}
					</td>
				</tr>
				</tbody>
			</table>
		</div>
	</div>

	<br />

	<!-- expandable course details -->

	<div class="panel-group expandable-course-details">
		<div class="panel panel-default">
			<div class="panel-heading">
				<h6 class="panel-title">
					<i class="icon-chevron-right icon-fixed-width"></i>
					<a data-toggle="collapse" data-parent=".expandable-course-details" href="#more-course-details">
						More Details
					</a>
				</h6>
			</div>
			<div id="more-course-details" class="panel-collapse collapse">
				<div class="panel-body data clearfix col1">
					<!-- extra course details -->
					<div class="data clearfix">
						<div class="col1">
							<table class="profile-or-course-info">
								<tbody>
								<tr>
									<th>Intended award</th>
									<td>${(studentCourseDetails.award.name)!}</td>
								</tr>
								<#if !isSelf>
								<tr>
									<th>Enrolment status</th>
									<td><@fmt.enrolment_status studentCourseDetails />
									</td>
								</tr>
								</#if>
								<tr>
									<th>Length of course</th>
									<td>
									<#if studentCourseDetails.courseYearLength??>
									${studentCourseDetails.courseYearLength} years
									</#if>
									<#if studentCourseDetails.modeOfAttendance??>
										<#if studentCourseYearDetails.modeOfAttendance.code != "F">
											(full-time equivalent)
										</#if>
									</#if>
									</td>
								</tr>
								<#if !isSelf>
								<tr>
									<th>Status on Course</th>
									<td><@fmt.status_on_course studentCourseDetails />
									</td>
								</tr>
								</#if>
								<tr>
									<th>Start date</th>
									<td>
									<#if studentCourseDetails.beginDate??>
												<@fmt.date date=studentCourseDetails.beginDate includeTime=false />
											</#if>
									</td>
								</tr>
								<tr>
									<th>End date</th>
									<td>
									<#if studentCourseDetails.endDate??>
										<@fmt.date date=studentCourseDetails.endDate includeTime=false />

									<#elseif studentCourseDetails.expectedEndDate??>
										<@fmt.date date=studentCourseDetails.expectedEndDate includeTime=false/> (expected)
									</#if>
									</td>
								</tr>
								<tr>
									<th>Programme route code</th>
									<td>${studentCourseDetails.sprCode}
									</td>
								</tr>
								<tr>
									<th>Course join code</th>
									<td>${studentCourseDetails.scjCode}
									</td>
								</tr>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</section>
