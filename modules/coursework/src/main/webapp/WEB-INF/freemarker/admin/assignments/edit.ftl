<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#-- 
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines. 
-->
<#assign commandName="editAssignmentCommand"/>
<#assign command=editAssignmentCommand />
<#assign canUpdateMarkingWorkflow=command.canUpdateMarkingWorkflow/>

<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/edit')}" commandName=commandName cssClass="form-horizontal">

<#--
<div id="form-editor-sidebar">

<@f.errors cssClass="error form-errors">
</@f.errors>

<div id="form-editor-tabs">

<div class="form-editor-tab" id="form-editor-addfield">
<h4>Generic form fields</h4>
<ul class="form-widget-list">
  <li class="widget widget-file">File attachment</li>
  <li class="widget widget-text">Text (1 line)</li>
  <li class="widget widget-textarea">Text area</li>
  <li class="widget widget-checkboxes">Checkboxes</li>
  <li class="widget widget-select">Select box</li>
  <li class="widget widget-radio">Multiple choice</li>
</ul>
</div>
<div class="form-editor-tab" id="form-editor-fieldprops">

</div>
<div class="form-editor-tab" id="form-editor-formprops">
-->
<@f.errors cssClass="error form-errors" />

<#assign newRecord=false />

<#include "_fields.ftl" />
<#--
</div>

</div>
</div>

<div id="form-editor-canvas">

</div>
-->
<div class="submit-buttons">
<input type="submit" value="Save" class="btn btn-primary">
or <a class="btn" href="<@routes.depthome module=assignment.module />">Cancel</a>
</div>
</@f.form>

<#if canDelete>
<p class="subtle">Did you create this assignment in error? 
You may <a href="<@routes.assignmentdelete assignment=assignment />" class="btn btn-danger">delete</a> it if you definitely won't need it again.</p>
<#else>
<p class="subtle">
It's not possible to delete this assignment, probably because it already has some submissions and/or published feedback.
</p>
</#if>

</#escape>