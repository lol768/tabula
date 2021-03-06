<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<#if nextStagesDescription??>
		<@cm2.assignmentHeader "Send to ${nextStagesDescription?lower_case}" assignment "for" />
	</#if>

	<@f.form method="post" action="${formAction}" modelAttribute="command" cssClass="double-submit-protection">
		<@form.errors path="" />
		<input type="hidden" name="confirmScreen" value="true" />

		<@spring.bind path="markerFeedback">
			<@bs3form.errors path="markerFeedback" />
			<#assign markerFeedback=status.actualValue />
			<#assign noContent = command.noContent />
			<#assign noMarks = command.noMarks />
			<#assign noFeedback = command.noFeedback />
			<#assign releasedFeedback = command.releasedFeedback />
			<#assign notReadyToMark = command.notReadyToMark />

			<#if releasedFeedback?has_content>
				<div class="alert alert-info">
						<#assign releasedFeedbackIds><ul><#list releasedFeedback as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul></#assign>
						<a class="use-popover"
							 data-html="true"
							 data-original-title="<span class='text-info'><strong>Already released</strong></span>"
							 data-content="${releasedFeedbackIds}">
							<@fmt.p (releasedFeedback?size ) "submission" />
						</a>
					<#if releasedFeedback?size == 1>
						has already been marked as completed. This will be ignored.
					<#else>
						have already been marked as completed. These will be ignored.
					</#if>
				</div>
			</#if>

			<#if notReadyToMark?has_content>
				<div class="alert alert-info">
					<#assign notReadyToMarkIds><ul><#list notReadyToMark as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul></#assign>
					<a class="use-popover"
						 data-html="true"
						 data-original-title="<span class='text-info'><strong>Already released</strong></span>"
						 data-content="${notReadyToMarkIds}">
						<@fmt.p (notReadyToMark?size ) "submission" />
					</a>
					<#if notReadyToMark?size == 1>
						is not ready for you to mark. This will be ignored.
					<#else>
						are not ready for you to mark. These will be ignored.
					</#if>
				</div>
			</#if>

			<#if (assignment.collectMarks && noMarks?size > 0)>
				<#assign count><#if (noMarks?size > 1)>	${noMarks?size} students do<#else>One student does</#if></#assign>
				<#assign noMarksIds><ul><#list noMarks as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul></#assign>
				<div class="alert alert-info">
					${count} not have a mark. You will not be able to add a mark for these students later.
					<a class="use-popover" id="popover-marks" data-html="true"
						 data-original-title="<span class='text-info'><strong>No marks</strong></span>"
						 data-content="${noMarksIds}">
						<i class="fa fa-question-sign"></i>
					</a>
				</div>
			</#if>

			<#if (noFeedback?size > 0) >
				<#assign count><#if (noFeedback?size > 1)>${noFeedback?size} students do<#else>One student does</#if></#assign>
				<#assign noFilesIds>
				<ul><#list noFeedback as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul>
				</#assign>
				<div class="alert alert-info">
					${count} not have any feedback files attached. You will not be able to add feedback comments or files for this student later.
					<a class="use-popover" id="popover-files" data-html="true"
						 data-original-title="<span class='text-info'><strong>No feedback files</strong></span>"
						 data-content="${noFilesIds}">
						<i class="fa fa-question-sign"></i>
					</a>
				</div>
			</#if>

			<#if (noContent?has_content)>
				<#assign count><#if (noContent?size > 1)>${noContent?size} students have<#else>One student has</#if></#assign>
				<#assign noContentIds>
				<ul><#list noContent as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul>
				</#assign>
				<div class="alert alert-info">
					${count} not been given a mark. These students will not be sent to the ${nextStagesDescription?lower_case}.
					<a class="use-popover" id="popover-files" data-html="true" aria-label="Help"
						 data-original-title="<span class='text-info'><strong>Not marked</strong></span>"
						 data-content="${noContentIds}">
						<i class="fa fa-question-circle"></i>
					</a>
				</div>
			</#if>

			<p>
				Feedback for <strong><@fmt.p (command.feedbackForRelease?size) "student" /></strong> will be listed as completed.
				Note that you will not be able to make any further changes to the marks or feedback associated with these students after this point.
				If there are still changes that have to be made for these students then click cancel to return to the feedback list.
			</p>

			<#list markerFeedback as mf>
				<input type="hidden" name="markerFeedback" value="${mf.id}" />
			</#list>
		</@spring.bind>

		<@bs3form.form_group>
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" /> I confirm that I have completed marking for <@fmt.p markerFeedback?size "this" "these" "1" "0" false /> students
			</@bs3form.checkbox>
		</@bs3form.form_group>

		<div class="buttons submit-buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class="btn btn-default" href="<@routes.cm2.listmarkersubmissions assignment marker />">Cancel</a>
		</div>
	</@f.form>
</#escape>