<%@ include file="/include.jsp"%>
<div><h3 class="title">Notification settings</h3></div>

<table class="settings">
  <thead>
 		<tr style="background-color: rgb(245, 245, 245);">
  	  <th class="name">URL</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <form action="notification/index.html?projectId=${projectId}" method="post">
        <input name="url" type=text size=64 maxlength=512 value="<c:out value="${url}"/>"/>
        <input class="submitButton" type="submit" value="Save"/>
      </form>
    </tr>
  </tbody>
</table>
