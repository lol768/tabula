<#assign tiles=JspTaglibs["http://tiles.apache.org/tags-tiles"]>
<!doctype html>
<html lang="en-GB">
	<head>
		<#include "_head.ftl" />
	</head>
	<body class="horizontal-nav layout-100 tabula-page ${component.bodyClass?default('component-page')} ${bodyClasses?default('')}">
		<div id="container">
			<#if (user.god)!false>
				<div id="god-notice" class="sysadmin-only-content">
					God mode enabled.
					<@f.form method="post" action="${url('/sysadmin/god', '/')}">
						<input type="hidden" name="returnTo" value="${info.requestedUri!""}" />
						<input type="hidden" name="action" value="remove" />
						<button class="btn btn-mini btn-info"><i class="icon-eye-close"></i> Disable God mode</button>
					</@f.form>
				</div>
			</#if>
			<#if (user.masquerading)!false>
				<div id="masquerade-notice" class="sysadmin-only-content">
					Masquerading as <strong>${user.apparentUser.fullName}</strong>. <a href="<@url page="/admin/masquerade" context="/"/>">Change</a>
				</div>
			</#if>
			<!-- Change this to header-medium or header-large as necessary - large is for homepages only -->
			<div id="header" class="<#if jumbotron?? && jumbotron>header-medium<#else>header-small</#if>" data-type="image">
			
				<div id="masthead">
					<div class="access-info">
      					<a href="#main-content" accesskey="c" title="Skip to content [c]">Skip to content</a>
      					<a href="#navigation" accesskey="n" title="Skip to navigation [n]">Skip to navigation</a>
    				</div>
				
					<!-- The on-hover class here specifies that the links should only be displayed on hover -->
					<div id="warwick-logo-container" class="on-hover">
						<a id="warwick-logo-link" href="http://www.warwick.ac.uk" title="University of Warwick homepage">
							<img id="warwick-logo" src="<@url resource="/static/images/logo.png" />" alt="University of Warwick">
						</a>
					</div>
					
					<div id="utility-container">
						<div id="utility-bar">
							<ul>
								<li>
								<#if user?? && user.loggedIn>
									Signed in as ${user.fullName}
									<#if user.staff>
									| <a href="/settings">Settings</a> 
									</#if>
									| <a class="sso-link" href="<@sso.logoutlink target="${rootUrl}" />">Sign out</a>
								<#else>
								    <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
								</#if>
								</li>
							</ul>
						</div>
					</div>
				</div>
				
				<div id="page-header">
					<div class="content">
						<div id="site-header-container">
							<h1 id="site-header">
							<#if component.subsite>
								<span id="parent-site-header">
									<a href="/" title="Tabula home page">Tabula</a>
								</span>
								<span id="subsite-character">&raquo;</span>
							</#if>
								<span id="current-site-header"><#compress>
									<#assign homeUrl><@url page="/" /></#assign>
									<#if (info.requestedUri != homeUrl)!false>
										<a href="${homeUrl}">${component.siteHeader?default('Tabula')}</a>
									<#else>
										<span>${component.siteHeader?default('Tabula')}</span>	
									</#if>
								</#compress></span>
							</h1>
							
							<h2 id="strapline">
								<#if jumbotron?? && jumbotron>
									Student management and administration system
								</#if>
							</h2>
						</div>
						
						<div id="custom-header">
							<div>
							<#if (info.maintenance)!false>
								<span id="maintenance-mode-label" class="label label-warning" rel="popover" title="System read-only" data-placement="left" data-content="This system has been placed in a read-only mode. You will be able to downoad files, but other operations are not currently possible. Normal access will be restored very soon.">Read-only</span>
								<script>
									jQuery(function($){
										$('#maintenance-mode-label').popover();
									});
								</script>
							</#if>
							</div>
						</div>
					</div>
				</div>
			</div>
			
			<div id="navigation-and-content">
			
				<#if !component.nonav>	
					<div id="navigation" class="horizontal">
						<div id="primary-navigation-wrapper">
							<div id="before-primary-navigation"></div>
				
							<div id="primary-navigation-container" >
								<ul id="primary-navigation" >
									<li class="section rendered-link">
										<div class="link-content">
											<div class="title rendered-link-content">
												<#assign homeUrl><@url page="/" /></#assign>
												<#if (info.requestedUri != homeUrl)!false>
													<a href="${homeUrl}">${component.title?default('Tabula')}</a>
												<#else>
													<span>${component.title?default('Tabula')}</span>	
												</#if>
											</div>
										</div>
									</li>
									<#if breadcrumbs??><#list breadcrumbs as crumb><li class="section rendered-link">
										<div class="link-content">
											<div class="title rendered-link-content">
												<a href="<@url page=crumb.url />" <#if crumb.tooltip??>title="${crumb.tooltip}"</#if>>${crumb.title}</a>										
											</div>
										</div>
									</li></#list></#if>
								</ul>
							</div>
				
				
							<div id="after-primary-navigation"></div>
						</div>
					</div>
				</#if>
		
				<div id="content-wrapper">					     
					<div id="main-content">
						<#--
						<div id="page-title">
			
							<h1>
								<span id="after-page-title"></span>
							</h1>
			
							<div id="page-title-bottom"></div>
						</div>
						-->
			
						<!-- column-1 and column-2 may not stick around as IDs - don't use them in a site design -->
						<div id="column-1"><div id="column-1-content">
						
						<@tiles.insertAttribute name="body" />
						
						</div></div>

						<div style="clear:both;"></div>
					</div>
				</div>
			
				<div style="clear:both;"></div>
			</div>
			
			<div style="clear:both;"></div>
			
			<div id="footer">
				<div class="content">
					<div id="custom-footer">
						<!-- Enter any custom footer content here (like contact details et al) -->
					</div>
					
					<div style="clear:both;"></div>
					
					<div id="common-footer">
						<div id="page-footer-elements" class="nofollow">
							<span class="footer-left"></span> <span class="footer-right"></span>
							
							<div style="clear:both;"></div>
						</div>
						
						<div id="footer-utility">
							<ul>
								<li id="sign-inout-link">
				          			<#if user?? && user.loggedIn>
										<a class="sso-link" href="<@sso.logoutlink target="${rootUrl}" />">Sign out</a>
									<#else>
									    <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
									</#if>
			          			</li>
			          			<li class="spacer">|</li>
			          			<li id="copyright-link"><a href="http://go.warwick.ac.uk/terms" title="Copyright Statement">&copy; <@warwick.copyright /></a></li>
			          			<li class="spacer">|</li>
			          			<li id="privacy-link"><a href="http://go.warwick.ac.uk/terms#privacy" title="Privacy statement">Privacy</a></li>
			          			<li class="spacer">|</li>
			          			<li id="accessibility-link"><a href="http://go.warwick.ac.uk/accessibility" title="Accessibility information [0]" accesskey="0">Accessibility</a></li>
			          			<li class="spacer subtle">|</li>
			          			<li class="subtle">
			          				App last built <@warwick.formatDate value=appBuildDate pattern="d MMMM yyyy HH:mm" />
			          			</li>
		          			</ul>
		          			
		          			<div id="app-feedback-link"><a href="/app/tell-us<#if info??>?currentPage=${info.requestedUri}</#if>">Give feedback</a></div>
		          				      					
<#if user?? && (user.sysadmin || user.masquerader)>
<div id="sysadmin-link">
<div class="btn-group">
	<a id="sysadmin-button" class="btn btn-inverse dropdown-toggle dropup" data-toggle="dropdown" href="<@url page="/sysadmin/" context="/" />"><i class="icon-cog icon-white"></i> System <span class="caret"></span></a>
	<ul class="dropdown-menu pull-right">
		<#if user.sysadmin>
		<li><a href="<@url page="/sysadmin/" context="/" />">Sysadmin home</a></li>
		</#if>
		<#if user.masquerader || user.sysadmin>
		<li><a href="<@url page="/admin/masquerade" context="/" />">Masquerade</a></li>
		</#if>
		<li><a href="#" id="hide-sysadmin-only-content">Hide sysadmin content</a></li>
	</ul>
</div>
</div>
<script type="text/javascript">
jQuery('#hide-sysadmin-only-content').on('click', function(){
  jQuery('#sysadmin-link').fadeOut('slow')
  jQuery('.sysadmin-only-content').hide('slow');
  return false;
});
</script>
</#if>
		          				      					
	      					<div style="clear:both;"></div>
		          		</div>
					</div>
				</div>
			</div>
		</div>
		
		
	</body>
</html>
