<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
<h1>Feedback forms for ${department.name}</h1>

<@f.form enctype="multipart/form-data"
		 method="post"
		 class="form-horizontal"
		 action="${url('/admin/department/${department.code}/settings/feedback-templates')}"
		 commandName="bulkFeedbackTemplateCommand">
	<@form.labelled_row "file.upload" "Upload feedback forms">
		<input type="file" name="file.upload" multiple />
		<div id="multifile-column-description" class="help-block">
			Your browser doesn't seem able to handle uploading multiple files<noscript>
			(or it does, but your browser is not running the Javascript needed to support it)</noscript>.
			A recent browser like Google Chrome or Firefox will be able to upload multiple files.
			You can still upload a single file here if you want.
			<div id="multifile-column-description-enabled" style="display:none">
				This uploader allows you to upload multiple files at once. They
				will need to be in the same folder on your computer for you to be
				able to select them all.
			</div>
		</div>
	</@form.labelled_row>
	<script>
		var frameLoad = function(frame){
			if(jQuery(frame).contents().find("form").length == 0){
				jQuery("#feedback-template-model").modal('hide');
				document.location.reload(true);
			}
		}

		jQuery(function($){
			if (Supports.multipleFiles) {
				jQuery('#multifile-column-description').html(jQuery('#multifile-column-description-enabled').html());
			}

			// models use ajax to retrieve their contents
			$('#feedback-template-list').on('click', 'a[data-toggle=modal]', function(e){
				$this = $(this);
				target = $this.attr('data-url');
				$("#feedback-template-model .modal-body").html('<iframe src="'+target+'" onLoad="frameLoad(this)" frameBorder="0"></iframe>')
			});

			$('#feedback-template-model').on('click', 'input[type=submit]', function(e){
				e.preventDefault();
				$('#feedback-template-model iframe').contents().find('form').submit();
			});
		});
	</script>
	<button type="submit" class="btn btn-primary">
		<i class="icon-upload icon-white"></i> Upload
	</button>
	<div class="submit-buttons">
		<#if bulkFeedbackTemplateCommand.feedbackTemplates?has_content>
			<table id="feedback-template-list" class="table table-striped table-bordered">
				<tr>
					<th>Name</th>
					<th>Description</th>
					<th>Assignments</th>
					<th><!-- Actions column--></th>
				</tr>
				<#list bulkFeedbackTemplateCommand.feedbackTemplates as template>
					<tr>
						<td>${template.name}</td>
						<td>${template.description!""}</td>
						<td>
							<#if template.hasAssignments>
								<span class="label label-info">${template.countLinkedAssignments}</span>&nbsp;
								<a id="tool-tip-${template.id}" class="btn btn-mini" data-toggle="button" href="#">
									<i class="icon-list"></i>
									List
								</a>
								<div id="tip-content-${template.id}" class="hide">
									<ul><#list template.assignments as assignment>
										<li>
											<a href="<@routes.depthome module=assignment.module />">
												${assignment.module.code} - ${assignment.name}
											</a>
										</li>
									</#list></ul>
								</div>
								<script type="text/javascript">
									jQuery(function($){
										var markup = $('#tip-content-${template.id}').html();
										$("#tool-tip-${template.id}").popover({
											placement: 'right',
											html: true,
											content: markup,
											title: 'Assignments linked to ${template.name}'
										});
									});
								</script>
							<#else>
								<span class="label">None</span>
							</#if>
						</td>
						<td>
							<#if template.attachment??>
							<a class="btn btn-mini" href="<@routes.feedbacktemplatedownload department=department feedbacktemplate=template />">
								<i class="icon-download"></i> Download
							</a>
							</#if>
							<a class="btn btn-mini" href="#feedback-template-model" data-toggle="modal" data-url="<@routes.feedbacktemplateedit department=department feedbacktemplate=template />">
								<i class="icon-pencil"></i> Edit
							</a>
							<#if !template.hasAssignments>
								<a class="btn btn-mini btn-danger" href="#feedback-template-model" data-toggle="modal" data-url="<@routes.feedbacktemplatedelete department=department feedbacktemplate=template />">
									<i class="icon-white icon-trash"></i> Delete
								</a>
							<#else>
								<a class="btn btn-mini btn-danger disabled" href="#" title="You cannot delete a feedback template with linked assignments">
									<i class="icon-white icon-trash"></i> Delete
								</a>
							</#if>
						</td>
					</tr>
				</#list>
			</table>
			<div id="feedback-template-model" class="modal fade">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
					<h3>Update feedback template</h3>
				</div>
				<div class="modal-body"></div>
				<div class="modal-footer">
					<input type="submit" value="Confirm" class="btn btn-primary">
					<a data-dismiss="modal" class="close-model btn" href="#">Cancel</a>
				</div>
			</div>
		</#if>
	</div>
</@f.form>
</#escape>