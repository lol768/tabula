<#--
 Don't forget to add a profile modal container when you use this macro if one doesn't exist on the page already
 <div id="profile-modal" class="modal fade profile-subset"></div>
 -->
<#macro profile_link member>
	<a class="ajax-modal profile-link hide" href="/profiles/view/subset/${member.universityId}" data-target="#profile-modal">
		<i class="icon-user"></i>
	</a>
</#macro>