<div class="monitoring-points">
	<@spring.bind path="command.monitoringPoints">
		<#if status.error>
			<div class="alert alert-error"><@f.errors path="command.monitoringPoints" cssClass="error"/></div>
		</#if>
	</@spring.bind>

	<#if command.monitoringPoints?size == 0>
		<div class="striped-section">
			<div class="pull-right">
				<a href="<@url page="/sysadmin/pointsettemplates/add/points/add?form=true" />" class="btn btn-primary new-point"><i class="icon-plus fa fa-plus"></i> Create new point</a>
			</div>
			<h2 class="section-title">Monitoring points</h2>
			<div class="striped-section-contents">
				<div class="item-info">
					<em>No points exist for this template</em>
				</div>
			</div>
		</div>
	<#else>
		<div class="striped-section">
			<div class="pull-right">
            	<a href="<@url page="/sysadmin/pointsettemplates/add/points/add?form=true" />" class="btn btn-primary new-point"><i class="icon-plus fa fa-plus"></i> Create new point</a>
            </div>
			<h2 class="section-title">Monitoring points</h2>
        	<div class="striped-section-contents">
        		<#assign pointCount = 0 />
                <#assign pointFields = ["name", "validFromWeek", "requiredFromWeek"] />
        		<#list command.monitoringPoints as point>
            		<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<a class="btn btn-primary edit-point" href="<@url page="/sysadmin/pointsettemplates/add/points/edit/${pointCount}?form=true"/>">Update</a>
								<a class="btn btn-danger delete-point" title="Delete" href="<@url page="/sysadmin/pointsettemplates/add/points/delete/${pointCount}?form=true"/>"><i class="icon-remove fa fa-times"></i></a>
							</div>
							${point.name} (week ${point.validFromWeek} - ${point.requiredFromWeek})

							<#list pointFields as pointField>
								<@spring.bind path="command.monitoringPoints[${pointCount}].${pointField}">
									<#if status.error>
										<div class="alert alert-error"><@f.errors path="command.monitoringPoints[${pointCount}].${pointField}" cssClass="error"/></div>
									</#if>
								</@spring.bind>
							</#list>

							<input type="hidden" name="monitoringPoints[${pointCount}].name" value="${point.name}" />
							<input type="hidden" name="monitoringPoints[${pointCount}].validFromWeek" value="${point.validFromWeek}" />
							<input type="hidden" name="monitoringPoints[${pointCount}].requiredFromWeek" value="${point.requiredFromWeek}" />
						</div>
					</div>
					<#assign pointCount = pointCount + 1 />
            	</#list>
        	</div>
        </div>
	</#if>
</div>