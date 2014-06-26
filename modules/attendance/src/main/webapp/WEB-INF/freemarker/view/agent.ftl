<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />
<#import "../attendance_macros.ftl" as attendance_macros />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<h1>${agent.fullName}'s ${relationshipType.studentRole}s</h1>

<div id="profile-modal" class="modal fade profile-subset"></div>

<div class="studentResults">
	<#assign returnTo><@routes.viewAgent department academicYear.startYear?c relationshipType agent /></#assign>
	<#assign returnTo = returnTo?url />
	<#if (result.totalResults > 0)>

		<@attendance_macros.scrollablePointsTable
			command=command
			filterResult=result
			visiblePeriods=visiblePeriods
			monthNames=monthNames
			department=department
			academicYear=academicYear
			returnTo=returnTo
			doCommandSorting=false
		; student>
			<#assign record_url><@routes.viewRecordStudent department academicYear.startYear?c student returnTo /></#assign>
			<@fmt.permission_button
				permission='MonitoringPoints.Record'
				scope=student
				action_descr='record monitoring points'
				classes='btn btn-primary btn-mini'
				href=record_url
				tooltip='Record'
			>
				<i class="icon-pencil icon-fixed-width late"></i>
			</@fmt.permission_button>
		</@attendance_macros.scrollablePointsTable>

		<div class="clearfix">
			<div class="pull-left">
				<@fmt.bulk_email_students students=result.students />
			</div>
		</div>

	<#else>
		<p>No students were found.</p>
	</#if>
</div>

<script>
	jQuery(window).on('load', function(){
		Attendance.scrollablePointsTableSetup();
	});
	jQuery(function($){
		Attendance.tableSortMatching([
			$('.scrollable-points-table .students'),
			$('.scrollable-points-table .attendance'),
			$('.scrollable-points-table .counts')
		]);
		$(".scrollable-points-table .students").tablesorter({
			sortList: [[2,0], [1,0]],
			headers: {0:{sorter:false}}
		});
		$(".scrollable-points-table .attendance").tablesorter({
			headers: {
				0:{sorter:false},
				1:{sorter:false},
				2:{sorter:false},
				3:{sorter:false},
				4:{sorter:false},
				5:{sorter:false}
			}
		});
		$(".scrollable-points-table .counts").tablesorter({
			headers: {2:{sorter:false}},
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.data('sortby')) {
					return $el.data('sortby');
				} else {
					return $el.text().trim();
				}
			}
		});
	});
</script>

</#escape>