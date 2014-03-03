/**
* Scripts used only by the attendance monitoring section.
*/
(function ($) { "use strict";

var exports = {};

exports.Manage = {};

exports.scrollablePointsTableSetup = function() {
    $('.scrollable-points-table .middle .sb-wide-table-wrapper').parent().addClass('scrollable');
    $('.scrollable-points-table .middle .scrollable').addClass('rightShadow').find('.sb-wide-table-wrapper').on('scroll', function(){
       var $this = $(this);
       if($this.scrollLeft() > 0) {
           $this.parent(':not(.leftShadow)').addClass('leftShadow');
       } else {
           $this.parent().removeClass('leftShadow');
       }
       if($this.scrollLeft() == 0 || $this.get(0).scrollWidth - $this.scrollLeft() != $this.outerWidth()) {
           $this.parent(':not(.rightShadow)').addClass('rightShadow');
       } else {
           $this.parent().removeClass('rightShadow');
       }
    });
};

exports.tableSortMatching = function(tableArray) {
    var matchSorting = function($sourceTable, targetTables){
        var $sourceRows = $sourceTable.find('tbody tr');
        $.each(targetTables, function(i, $table){
            var $tbody = $table.find('tbody');
            var oldRows = $tbody.find('tr').detach();
            $.each($sourceRows, function(j, row){
                var $sourceRow = $(row);
                oldRows.filter(function(){ return $(this).data('sortId') == $sourceRow.data('sortId'); }).appendTo($tbody);
            });
        });
    };

    if (tableArray.length < 2)
        return;

    $.each(tableArray, function(i){
        var otherTables = tableArray.slice();
        otherTables.splice(i, 1);
        this.on('sortEnd', function(){
            matchSorting($(this), otherTables);
        }).find('tbody tr').each(function(i){ $(this).data('sortId', i); });
    });
};

exports.bindModulePickers = function(){
	$('.module-search-query')
		.closest('.module-choice')
			.find('button.add-module').attr('disabled', true).end()
			.find('.modules-list').on('click', 'button.btn.btn-danger', function(){
				$(this).closest('li').remove();
			}).end()
		.end()
		.each(function(){
			var $this = $(this),
				$parent = $this.closest('.module-choice'),
				$addButton = $parent.find('button.add-module'),
				$hiddenInput = $parent.find('input[type=hidden]:first');
			$this.on('change', function(){
				if ($(this).data('moduleid') && $(this).data('moduleid').length === 0) {
					$addButton.attr('disabled', true);
				} else {
					$addButton.attr('disabled', false);
				}
			});

			$addButton.on('click', function(){
				$this.closest('.module-choice').find('input.specific[name=isAnySmallGroupEventModules]').attr('checked', true);
				var icon = $('<i/>').addClass('icon-fixed-width');
				if (!$this.data('hasgroups')) {
					icon.addClass('icon-exclamation-sign').attr({
						'title':'This module has no small groups set up in Tabula'
					});
				}
				$this.closest('.module-choice').find('.modules-list ul').append(
					$('<li/>').append(
							$('<input/>').attr({
								'type':'hidden',
								'name':$hiddenInput.attr('name').replace('_',''),
								'value':$this.data('moduleid')
							})
						).append(
							icon
						).append(
							$('<span/>').attr('title', $this.val()).html($this.val())
						).append(
							$('<button/>').addClass('btn btn-danger').append(
								$('<i/>').addClass('icon-remove')
							)
						)
				);
				$this.val('').data('moduleid','');
			});
	});
	$('.pointTypeOption.smallGroup .module-search-query').modulePicker({
		checkGroups: true
	});
	$('.pointTypeOption.assignmentSubmission .module-search-query').modulePicker({
		checkAssignments: true
	});
};

exports.bindAssignmentPickers = function(){
	$('.assignment-search-query')
		.closest('.assignment-choice')
			.find('button.add-assignment').attr('disabled', true).end()
			.find('.assignments-list').on('click', 'button.btn.btn-danger', function(){
				$(this).closest('li').remove();
			}).end()
		.end()
		.each(function(){
			var $this = $(this),
				$parent = $this.closest('.assignment-choice'),
				$addButton = $parent.find('button.add-assignment'),
				$hiddenInput = $parent.find('input[type=hidden]:first');
			$this.on('change', function(){
				if ($(this).data('assignmentid') && $(this).data('assignmentid').length === 0) {
					$addButton.attr('disabled', true);
				} else {
					$addButton.attr('disabled', false);
				}
			});

			$addButton.on('click', function(){
				$this.closest('.assignment-choice').find('.assignment-list ul').append(
					$('<li/>').append(
							$('<input/>').attr({
								'type':'hidden',
								'name':$hiddenInput.attr('name').replace('_',''),
								'value':$this.data('assignmentid')
							})
						).append(
							$('<span/>').attr('title', $this.val()).html($this.val())
						).append(
							$('<button/>').addClass('btn btn-danger').append(
								$('<i/>').addClass('icon-remove')
							)
						)
				);
				$this.val('').data('assignmentid','');
			});
		});

	$('.pointTypeOption.assignmentSubmission .assignment-search-query').assignmentPicker({});
};

$(function(){

	// SCRIPTS FOR MANAGING MONITORING POINTS

	if ($('#chooseCreateType').length > 0 && setsByRouteByAcademicYear) {
		var $form = $('#chooseCreateType'),
		    academicYearSelect = $form.find('select.academicYear'),
		    routeSelect = $form.find('select.route'),
		    yearSelect = $form.find('select.copy[name="existingSet"]');

		$form.find('input.create').on('change', function(){
			if ($(this).val() === 'copy') {
				$form.find('button').html('Copy').prop('disabled', true);
				$form.find('.existingSetOptions').css('visibility','hidden');
				$form.find('select.template[name="existingSet"]').prop('disabled', true);
				academicYearSelect.add(routeSelect).add(yearSelect).prop('disabled', false).css('visibility','hidden');
				academicYearSelect.find('option:first').prop('selected', true);
				academicYearSelect.css('visibility','visible');
			} else if ($(this).val() === 'template') {
				$form.find('button').html('Create').prop('disabled', false);
				$form.find('.existingSetOptions').css('visibility','visible');
                $form.find('select.template[name="existingSet"]').prop('disabled', false);
                academicYearSelect.add(routeSelect).add(yearSelect).prop('disabled', true).css('visibility','hidden');
			} else {
				$form.find('button').html('Create').prop('disabled', false);
				$form.find('.existingSetOptions').css('visibility','hidden');
				$form.find('select.template[name="existingSet"]').prop('disabled', true)
				academicYearSelect.add(routeSelect).add(yearSelect).prop('disabled', true).css('visibility','hidden');
			}
		});

		$form.on('submit', function(){
			var createType = $form.find('input.create:checked').val();
			if (createType === 'copy') {
				$form.find('select[name="existingSet"]').not('.copy').prop('disabled', true);
			} else if (createType === 'template') {
				$form.find('select[name="existingSet"]').not('.template').prop('disabled', true);
			} else {
				$form.find('select[name="existingSet"]').prop('disabled', true);
			}
		});

		academicYearSelect.on('change', function(){
			routeSelect.find('option:gt(0)').remove()
				.end().css('visibility','visible');
			yearSelect.find('option:gt(0)').remove()
				.end().css('visibility','hidden');
			$.each(academicYearSelect.find('option:selected').data('routes'), function(i, route){
				routeSelect.append(
					$('<option/>').data('sets', route.sets).html(route.code.toUpperCase() + ' ' + route.name)
				);
			});
		});

		routeSelect.on('change', function(){
			yearSelect.find('option:gt(0)').remove()
				.end().find('option:first').prop('selected', true)
				.end().css('visibility','visible').change();
			$.each(routeSelect.find('option:selected').data('sets'), function(i, set){
				yearSelect.append(
					$('<option/>').val(set.id).html(set.year)
				);
			});
		});

		yearSelect.on('change', function(){
			if (yearSelect.val() && yearSelect.val().length > 0) {
				$form.find('button').prop('disabled', false);
			} else {
				$form.find('button').prop('disabled', true);
			}
		});

		// Update Preview button to preview the currently selected template.
		$form.find('select.template[name=existingSet]').on('change', function() {
			var $button = $('.monitoring-point-preview-button'),
			    newHref = $button.data('hreftemplate').replace('_TEMPLATE_ID_', this.value);
			$button.attr('href', newHref);
		}).trigger('change');

		// Trigger the above change behaviour for the initial value
		$form.find('input.create:checked').trigger('change');

		if (academicYearSelect.length > 0) {
			for (var academicYear in setsByRouteByAcademicYear) {
				academicYearSelect.append(
					$('<option/>').data('routes', setsByRouteByAcademicYear[academicYear]).html(academicYear)
				);
			}
		}
	}

	var setupCollapsible = function($this, $target){
		$this.css('cursor', 'pointer').on('click', function(){
			var $chevron = $this.find('i.icon-fixed-width');
			if ($this.hasClass('expanded')) {
				$chevron.removeClass('icon-chevron-down').addClass('icon-chevron-right');
				$this.removeClass('expanded');
				$target.hide();
			} else {
				$chevron.removeClass('icon-chevron-right').addClass('icon-chevron-down');
				$this.addClass('expanded');
				$target.show();
			}
		});
		if ($this.hasClass('expanded')) {
			$target.show();
		} else {
			$target.hide();
		}
	};

	var updateRouteCounter = function(){
		var total = $('.routeAndYearPicker').find('div.scroller tr').filter(function(){
			return $(this).find('input:checked').length > 0;
		}).length;
		if (total === 0) {
			$('.routeAndYearPicker').find('span.routes-count').html('are no routes');
		} else if (total === 1) {
			$('.routeAndYearPicker').find('span.routes-count').html('is 1 route');
		} else {
         	$('.routeAndYearPicker').find('span.routes-count').html('are ' + total + ' routes');
         }
	};

	$('.striped-section.routes .collapsible').each(function(){
		var $this = $(this), $target = $this.closest('.item-info').find('.collapsible-target');
		setupCollapsible($this, $target);
	});

	$('.routeAndYearPicker').find('.collapsible').each(function(){
		var $this = $(this), $target = $this.parent().find('.collapsible-target');
		if ($target.find('input:checked').length > 0) {
			$this.addClass('expanded');
			updateRouteCounter();
		}
		setupCollapsible($this, $target);
	}).end().find('tr.years th:gt(0)').each(function(){
		var $this = $(this), oldHtml = $this.html();
		$this.empty().append(
			$('<input/>').attr({
				'type':'checkbox',
				'title':'Select all/none'
			}).on('click', function(){
				var checked = $(this).prop('checked');
				$this.closest('div').find('div.scroller tr').each(function(){
					$(this).find('td.year_' + $this.data('year') + ' input').not(':disabled').prop('checked', checked);
				});
				updateRouteCounter();
			})
		).append(oldHtml);
	}).end().find('td input:not(:disabled)').on('click', function(){
		var $this = $(this);
		$this.closest('div.collapsible-target').find('th.year_' + $this.data('year') + ' input').prop('checked', false);
		updateRouteCounter();
	});

	var pointsChanged = false;

	var bindPointButtons = function(){
    	$('.modify-monitoring-points a.new-point, .modify-monitoring-points a.edit-point, .modify-monitoring-points a.delete-point')
    		.off('click').not('.disabled').on('click', function(e){

    		e.preventDefault();
    		var formLoad = function(data){
    			var $m = $('#modal');
    			$m.html(data);
    			if ($m.find('.monitoring-points').length > 0) {
    				$('.modify-monitoring-points .monitoring-points').replaceWith($m.find('.monitoring-points'))
    				$m.modal("hide");
    				bindPointButtons();
    				pointsChanged = true;
    				return;
    			}
    			var $f = $m.find('form');
    			$f.on('submit', function(event){
    				event.preventDefault();
    			});
    			$m.find('.modal-footer button[type=submit]').on('click', function(){
    				$(this).button('loading');
    				$.post($f.attr('action'), $f.serialize(), function(data){
    					$(this).button('reset');
    					formLoad(data);
    				})
    			});
    			$m.off('shown').on('shown', function(){
    				$f.find('input[name="name"]').focus();
    			}).modal("show");
    		};
    		if ($('form#addMonitoringPointSet').length > 0) {
    			$.post($(this).attr('href'), $('form#addMonitoringPointSet').serialize(), formLoad);
    		} else {
    			$.get($(this).attr('href'), formLoad);
    		}
    	});
    };
    bindPointButtons();

    $('#addMonitoringPointSet').on('submit', function(){
    	pointsChanged = false;
    })

    if ($('#addMonitoringPointSet').length > 0) {
    	$(window).bind('beforeunload', function(){
    		if (pointsChanged) {
    			return 'You have made changes to the monitoring points. If you continue they will be lost.'
    		}
		});
	}

	// END SCRIPTS FOR MANAGING MONITORING POINTS



});

window.Attendance = jQuery.extend(window.Attendance, exports);

}(jQuery));

