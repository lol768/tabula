<#escape x as x?html>
<h1>All submissions and feedback for ${assignment.name}</h1>

<#assign module=assignment.module />

<div>
<a class="btn long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions/download-zip/submissions.zip'/>"><i class="icon-download"></i>
Download submissions
</a>
<a class="btn long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download-zip/feedback.zip'/>"><i class="icon-download"></i>
Download feedback
</a>
<a class="btn" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions.xml'/>"><i class="icon-download"></i>
XML
</a>
<a class="btn btn-danger" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/delete' />" id="delete-selected-button">Delete</a>

<#if features.turnitin>
<a class="btn" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/turnitin' />" id="turnitin-submit-button">Submit to Turnitin</a>
</#if>

<a class="btn btn-warn" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised' />" id="mark-plagiarised-selected-button">Mark selected plagiarised</a>

</div>
<#macro originalityReport r>
<img src="<@url resource="/static/images/icons/turnitin-16.png"/>">
<span class="similarity-${r.similarity}">${r.overlap}% similarity</span>
<span class="similarity-subcategories">
(Web: ${r.webOverlap}%,
Student papers: ${r.studentOverlap}%,
Publications: ${r.publicationOverlap}%)
</span>
</#macro>

<#if students?size = 0>
	<p>There are no submissions or feedbacks yet for this assignment.</p>
<#else>
<div class="submission-feedback-list">
	<@form.selector_check_all />
	<table id="submission-table" class="table table-bordered table-striped">
		<tr>
			<th></th>
			<th>Student</th>
			<th>Submitted</th>
			<th>Submission Status</th>
			<#if assignment.collectMarks>
				<th>Mark</th>
				<th>Grade</th>
			</#if>
			<th>Files</th>
			<th>Feedback</th>
			<th>Uploaded</th>
			<th>Feedback Status</th>
			<#if hasOriginalityReport><th>Originality Report</th></#if>
		</tr>
		<#list students as student>
			<#assign enhancedSubmission=student.enhancedSubmission>
			<#assign submission=enhancedSubmission.submission>
			<#assign feedback=student.feedback>
			
			<tr class="itemContainer" <#if submission.suspectPlagiarised> data-plagiarised="true" </#if> >
				<td><@form.selector_check_row "students" student.uniId /></td>
				<td class="id">${student.uniId}</td>
				<#-- TODO show student name if allowed by department --> 
				<td class="submitted">
					<span class="date">
						<#if submission.submittedDate??>
							<@fmt.date date=submission.submittedDate seconds=true capitalise=true />
						</#if>
					</span>
				</td>
				<td class="submission-status">
					<#if submission.late>
						<span class="label-red">Late</span>
					<#elseif  submission.authorisedLate>
						<span class="label-blue">Authorised Late</span>
					</#if>
					<#if enhancedSubmission.downloaded>
						<span class="label-green">Downloaded</span>
					</#if>
					<#if submission.suspectPlagiarised>
						<span class="label-orange">Suspect Plagiarised</span>
					</#if>
				</td>
				 <#if assignment.collectMarks>
                    <td class="mark">
                        <#if feedback.actualMark??>${feedback.actualMark}</#if>
                    </td>
                    <td class="grade">
                        <#if feedback.actualGrade??>${feedback.actualGrade}</#if>
                    </td>
                </#if>
				<td nowrap="nowrap" class="files">
					Files download link will go here
				</td>
				<td nowrap="nowrap" class="download">
					<#if feedback.attachments?size gt 0>
					<a class="btn long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download/${feedback.id}/feedback-${feedback.universityId}.zip'/>">
						<i class="icon-download"></i>
						${feedback.attachments?size}
						<#if feedback.attachments?size == 1> file
						<#else> files
						</#if>
					</a>
					</#if>
				</td>
				
				<td class="uploaded"><@fmt.date date=feedback.uploadedDate seconds=true capitalise=true /></td>
				<td class="feedbackReleased">
					<#if feedback.released>Published
					<#else>Not Yet Published
					</#if>
				</td>
				<#if hasOriginalityReport>
					<td class="originality-report">
						<#if submission.originalityReport??>
							<@originalityReport item.submission.originalityReport />
						</#if>
					</td>
				</#if>
			</tr>
		</#list>
	</table>
</div>
</#if>
</#escape>
