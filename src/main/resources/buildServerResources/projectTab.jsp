<%@ include file="/include.jsp"%>
<div><h3 class="title">Notification URL</h3></div>

<table class="settings">
  <tr>
    <form action="${action}" method="post">
      <input name="projectId" type=hidden value="${projectId}"/>
      <input name="url" type=text value="${url}" size=64 maxlength=512/>
      <input class="submitButton" type="submit" value="Save"/>
    </form>
  </tr>
</table>
