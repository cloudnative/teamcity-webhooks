<%@ include file="/include.jsp"%>
<div><h3 class="title">Webhook URL</h3></div>

<table class="settings" style="width: auto; border: 0">
<%-- Model filled by WebhooksProjectTab --%>
<c:if test="${canEdit}">
<form action="${action}" method="post">
  <input name="projectId" type="hidden" value="${projectId}"/>
</c:if>
  <c:forEach items="${urls}" var="url" varStatus="j">
    <tr>
    <c:choose>
      <c:when test="${canEdit}">
        <td <c:if test="${j.count == 1}">style="border-top: none"</c:if>><input name="url${j.count}" type="text" value="<c:out value="${url}"/>" size="64" maxlength="256"/></td>
        <td <c:if test="${j.count == 1}">style="border-top: none"</c:if> class="noRightBorder"><button name="delete" value="url${j.count}" class="submitButton" type="submit">Delete</button></td>
      </c:when>
      <c:otherwise>
        <td <c:if test="${j.count == 1}">style="border-top: none"</c:if> class="noRightBorder"><span><c:out value="${url}"/></span></td>
      </c:otherwise>
    </c:choose>
    </tr>
  </c:forEach>
  <c:if test="${canEdit}">
    <tr>
      <td <c:if test="${urls.size() < 1}">style="border-top: none"</c:if>><input name="new-url" type="text" value="" size="64" maxlength="256" autofocus/></td>
      <td <c:if test="${urls.size() < 1}">style="border-top: none"</c:if> class="noRightBorder"><button name="add" value="new-url" class="submitButton" type="submit">Add</button></td>
    </tr>
  </c:if>
<c:if test="${canEdit}">
</form>
</c:if>
</table>
