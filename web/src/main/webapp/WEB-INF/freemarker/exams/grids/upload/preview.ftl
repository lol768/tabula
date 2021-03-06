<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.exams.uploadYearMarks dept academicYear /></#local>
	<#return result />
</#function>

<@fmt.id7_deptheader title="Submit year marks for ${academicYear.toString}" route_function=route_function preposition="in" />

<div class="fix-area">
	<#if invalidItems?has_content>
		<div class="alert alert-danger">
			<#if validItems?has_content>
				<p>
					<@fmt.p invalidItems?size "row is" "rows are" /> invalid and cannot be uploaded.
				</p>
				<p>
					<button class="btn btn-default" id="toggleInvalidRows">Show details</button>
				</p>
			<#else>
				There are no valid rows to upload.
			</#if>
		</div>

		<table class="table table-striped table-condensed" id="invalidRows"<#if validItems?has_content> style="display: none"</#if>>
			<thead>
				<tr>
					<th>Student ID</th>
					<th>Mark</th>
					<th>Errors</th>
				</tr>
			</thead>
			<tbody>
				<#list invalidItems as row>
					<tr>
						<td>${row.yearMarkItem.studentId!}</td>
						<td>${row.yearMarkItem.mark!}</td>
						<td><#list row.errors![] as error>
							${error}<#if error_has_next><br /></#if>
						</#list></td>
					</tr>
				</#list>
			</tbody>
		</table>

		<script>
			jQuery(function ($) {
				$('#toggleInvalidRows').on('click', function () {
					$(this).text($('#invalidRows').is(':visible') ? 'Show details' : 'Hide details');
					$('#invalidRows').toggle();
				});
			});
		</script>
	</#if>

	<#if defaultAcademicYear?has_content || guessedSCJ?has_content || roundedMark?has_content>
		<div class="alert alert-info">
			<#if guessedSCJ?has_content>
				<p><@fmt.p guessedSCJ?size "row" /> specified a University ID so the appropriate SCJ code has been chosen based on the academic year.</p>
			</#if>
			<#if roundedMark?has_content>
				<p><@fmt.p roundedMark?size "row" /> had a mark with greater than one decimal place. These have been rounded.</p>
			</#if>
			<#if defaultAcademicYear?has_content>
				<p><@fmt.p defaultAcademicYear?size "row" /> did not specify an academic year. These have been set to <strong>${academicYear.toString}</strong>.</p>
			</#if>
		</div>
	</#if>

	<#if validItems?has_content>
		<#function rowInfo item>
			<#local infos = [] />
			<#if item.scjCode != item.yearMarkItem.studentId>
				<#local infos = infos + ['University ID specified'] />
			</#if>
			<#if item.mark?string != item.yearMarkItem.mark>
				<#local infos = infos + ['Mark rounded (' + item.yearMarkItem.mark + ')'] />
			</#if>
			<#if !item.yearMarkItem.academicYear?has_content>
				<#local infos = infos + ['Default academic year applied'] />
			</#if>
			<#return infos />
		</#function>
		<#assign formUrl><@routes.exams.uploadYearMarks department academicYear /></#assign>
		<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" modelAttribute="command">
			<table class="table table-striped table-condensed">
				<thead>
					<tr>
						<th>Student ID</th>
						<th>Mark</th>
						<th>Academic year</th>
						<th></th>
					</tr>
				</thead>
				<tbody>
					<#list validItems as item>
						<#assign infos = rowInfo(item) />
						<tr <#if infos?has_content>class="info"</#if>>
							<td>${item.scjCode}</td>
							<td>${item.mark}</td>
							<td>${item.academicYear.toString}</td>
							<td><#list infos as info>
								${info}<#if info_has_next><br /></#if>
							</#list></td>
						</tr>
						<input type="hidden" name="marks[${item_index}].studentId" value="${item.scjCode}" />
						<input type="hidden" name="marks[${item_index}].mark" value="${item.mark}" />
						<input type="hidden" name="marks[${item_index}].academicYear" value="${item.academicYear.toString}" />
					</#list>
				</tbody>
			</table>

			<div class="submit-buttons fix-footer">
				<button class="btn btn-primary" type="submit" name="confirm">Confirm</button>
				<a class="btn btn-default" href="<@routes.exams.gridsDepartmentHomeForYear department academicYear />">Cancel</a>
			</div>
		</@f.form>
	<#else>
		<div class="submit-buttons fix-footer">
			<a class="btn btn-default" href="<@routes.exams.gridsDepartmentHomeForYear department academicYear />">Cancel</a>
		</div>
	</#if>

</div>

<script>
	(function($) {
		$('.fix-area').fixHeaderFooter();
	})(jQuery);
</script>


</#escape>