<@bs3form.labelled_form_group "fields[${field.id}].value" "Feedback" help>
	<@form.field>
		<@f.textarea id="fields[${field.id}].value" cssClass="big-textarea form-control" path="fields[${field.id}].value" />
	</@form.field>
	<script>
		jQuery(function ($) {
			var previewBlock = $("#preview-block-${field.id}-${command.student.userId}");
			var feedbackTextArea = $("form[studentid=${command.student.userId}] textarea");
			var previewText = $("div[id='${field.id}-${command.student.userId}-preview-content']");

			function trimPTag(inputString) {
				return inputString.replace('<p>', '').replace('</p>', '').trim();
			}

			// do not render the preview block if they look the same
			function hidePreviewIfNotNeeded() {
				if (trimPTag(previewText.html()) === feedbackTextArea.val().trim()) {
					previewBlock.hide();
				} else {
					previewBlock.show();
				}
			}

			previewBlock.hide();

			var timeout = null;
			feedbackTextArea.keyup(function () {
				if (timeout != null) {
					clearTimeout(timeout);
				}
				timeout = setTimeout(function () {
					timeout = null;
					if (trimPTag(previewText.html()) !== feedbackTextArea.val().trim()) {
						$.ajax('/markdown/toHtml', {
							'type': 'POST',
							'data': {
								markdownString: feedbackTextArea.val()
							},
							success: function (res) {
								// update content as long as they are different
								if (res.trim() !== previewText.html().trim()) {
									previewText.html(res);
								}
								hidePreviewIfNotNeeded();
							}
						});
					}
				}, 300);
			});
		});
	</script>
	<#if showHelpText?? && showHelpText>
		<div class="help-block">
			You can use Markdown syntax <a target="_blank" href="https://warwick.ac.uk/tabula/manual/cm2/markers/markdown/"><i class="icon-question-sign fa fa-question-circle"></i></a>
		</div>
	</#if>
</@bs3form.labelled_form_group>

<div id="preview-block-${field.id}-${command.student.userId}">
	<label>Preview</label>
	<div class="well">
		<div id="${field.id}-${command.student.userId}-preview-content"></div>
	</div>
</div>