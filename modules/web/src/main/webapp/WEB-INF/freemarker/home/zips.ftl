<#escape x as x?html>

<h1>Download ${zipType} ZIP file</h1>

<div class="alert alert-info">
	<p>The ZIP file is currently being generated. You can download it below when it is ready.</p>

	<div class="progress progress-striped active">
		<div class="bar" style="width: 0;"></div>
	</div>

	<p class="zip-progress">Initialising</p>
</div>

<div class="zip-complete alert alert-success" style="display: none;">
	<h3>ZIP file generated successfully</h3>
	<p><a href="<@routes.zipComplete jobId />" class="btn"><i class="icon-download fa fa-arrow-circle-o-down"></i> Download ZIP file</a></p>
</div>

<a class="btn" href="${returnTo}">Done</a>

<script>
	jQuery(function($){
		var updateProgress = function() {
			$.get('<@routes.zipProgress jobId />', function(data){
				if (data.succeeded) {
					$('.progress .bar').width("100%");
					$('.zip-progress').empty();
					$('.zip-complete').show();
					$('.progress').removeClass('active progress-striped')
				} else {
					$('.progress .bar').width(data.progress + "%");
					if (data.status) {
						$('.zip-progress').html(data.status);
					}
					setTimeout(updateProgress, 5 * 1000);
				}
			});
		};
		updateProgress();
	});
</script>

</#escape>