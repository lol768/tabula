<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign submitUrl><@routes.coursework.addFeedbackSingle assignment /></#assign>
<@f.form method="post" enctype="multipart/form-data" action=submitUrl modelAttribute="addFeedbackCommand">

<h1>Submit feedback</h1>
<h5><span class="muted">for</span> ${assignment.name}</h5>

<@form.labelled_row "uniNumber" "Student university number">
<@f.input path="uniNumber" cssClass="text" />
</@form.labelled_row>

<@form.filewidget basename="file"  />

<div class="submit-buttons">
<input class="btn btn-primary" type="submit" value="Submit">
</div>
</@f.form>

</#escape>