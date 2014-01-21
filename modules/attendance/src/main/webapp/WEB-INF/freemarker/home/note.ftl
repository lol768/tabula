<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

	<#if isModal>
		<@modal.header>
			<h2>Edit attendance note</h2>
		</@modal.header>
	<#elseif isIframe>
		<div id="container">
	<#else>
		<h2>Edit attendance note</h2>
	</#if>

	<#if isModal>
		<@modal.body />

		<@modal.footer>
			<form class="double-submit-protection">
				<span class="submit-buttons">
					<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Saving&hellip;">
						Save
					</button>
					<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
				</span>
			</form>
		</@modal.footer>
	<#else>

		<@f.form id="attendance-note-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="form-horizontal double-submit-protection">

			<@form.labelled_row "note" "Note">
				<@f.textarea path="note" cssClass="input-block-level" rows="5" cssStyle="height: 150px;" />
			</@form.labelled_row>

			<#if command.attachedFile?has_content>
				<@form.labelled_row "attachedFile" "Attached file">
					<i class="icon-file-alt"></i>
					<@fmt.download_link
						filePath="/note/${command.student.universityId}/${command.monitoringPoint.id}/attachment/${command.attachedFile.name}"
						mimeType=command.attachedFile.mimeType
						title="Download file ${command.attachedFile.name}"
						text="Download ${command.attachedFile.name}"
					/>
					&nbsp;
					<@f.hidden path="attachedFile" value="${command.attachedFile.id}" />
					<i class="icon-remove-sign remove-attachment"></i>

					<script>
						jQuery(function($){
							$(".remove-attachment").on("click", function(e){
								$(this).closest(".control-group").remove();
								return false;
							});
						});
					</script>
					<small class="subtle help-block">
						This is the file attachmented to this administrative note.
						Click the remove link next to a document to delete it.
					</small>
				</@form.labelled_row>
			</#if>

			<@form.filewidget basename="file" types=[] multiple=false />

			<#if isIframe>
				<input type="hidden" name="isModal" value="true" />
			<#else>

				<div class="form-actions">
					<div class="pull-right">
						<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
						<a class="btn" href="${returnTo}">Cancel</a>
					</div>
				</div>

			</#if>

		</@f.form>

	</#if>

	<#if isIframe>
		</div> <#--container -->
	</#if>

</#escape>