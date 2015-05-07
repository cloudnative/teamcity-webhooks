<%@ include file="/include.jsp"%>
<div><h3 class="title">Webhook URL</h3></div>

<table class="settings">
<%-- Model filled by WebhooksProjectTab --%>
<form action="${action}" method="post">
  <input name="projectId" type="hidden" value="${projectId}"/>
  <c:forEach items="${urls}" var="url" varStatus="j">
    <tr>
      <td><input name="url${j.count}" type="text" value="${url}" size="64" maxlength="256"/></td>
      <td><button name="delete" value="url${j.count}" class="submitButton" type="submit">Delete</button></td>
    </tr>
  </c:forEach>
  <tr>
    <td><input name="new-url" type="text" value="" size="64" maxlength="256" autofocus/></td>
    <td><button name="add" value="new-url" class="submitButton" type="submit">Add</button></td>
  </tr>
</form>
</table>
