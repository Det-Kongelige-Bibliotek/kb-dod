<%@ page import="dk.kb.alma.gen.Request" %>
<%@ page import="dk.kb.alma.gen.User" %>
<%@ page import="dk.kb.dod.facade.DodFacade" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="org.springframework.web.servlet.support.RequestContextUtils" %>

<%
    ApplicationContext ac = RequestContextUtils.findWebApplicationContext(request);
    DodFacade facade = ac.getBean(DodFacade.class);
%>
<html>
<head>
    <title>Finish DoD</title>
</head>
<body>
  <ul>
      <p>
          <%
              String bookUrl = request.getParameter("bookUrl");
              String mmsID = request.getParameter("mmsID");
              Request firstRequest = facade.getRequest(mmsID).getRequests().get(0);
              String title = facade.getBib(mmsID).getTitle();
              String requestID = firstRequest.getId();
              String user = firstRequest.getRequester().getValue();
              User bibUser = facade.getUser(user);
              String email = bibUser.getContactInfo().getEmails().getEmails().get(0).getEmailAddress();
          %>


          Record Id: <%= mmsID %>
          <br>
          Request Id:
          <%= requestID %>
          <br>
          User Name:
          <%= user %>
          <br>
          User Email:
          <%= email %>
          <% facade.sendMail(user, email, title, bookUrl); %>
      </p>
  </ul>
</body>
</html>
