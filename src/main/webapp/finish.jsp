<%@ page import="dk.kb.dod.AlmaClient" %>
<%@ page import="dk.kb.alma.gen.User" %>
<%@ page import="dk.kb.dod.SendMail" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.FileInputStream" %>
<html>
<head>
    <title>Finish DoD</title>
</head>
<body>
  <ul>
      <p>
          <%
              Properties dodpro = new Properties();
              try {
                  dodpro.load(new FileInputStream("resources/dod.properties"));
              } catch (IOException e) {
                  e.printStackTrace();
              }
              String apikey = dodpro.getProperty("alma.apikey");    //"l8xx570d8eccc65b4fc3a8fbb512784181bd";
              String url = dodpro.getProperty("alam.url");    //"https://api-eu.hosted.exlibrisgroup.com/almaws/v1/";
              AlmaClient almaClient = new AlmaClient(url, apikey);
              String bookUrl = request.getParameter("bookUrl");
              String id = request.getParameter("bookId");
              String title = almaClient.getBibRecord(id).getTitle();
          %>
          Record Id: <%= id %>
          <br>
          Request Id:
          <%= almaClient.getRequests(id).getUserRequest().get(0).getRequestId()%>
          <br>
          <% String user = almaClient.getRequests(id).getUserRequest().get(0).getUserPrimaryId(); %>
          User Name:
          <%= user %>
          <br>
          User Email:
          <% User bibUser = almaClient.getUser(user);%>
          <% String email = bibUser.getContactInfo().getEmails().getEmail().get(0).getEmailAddress();%>
          <%= email %>
          <% email="nkh@kb.dk"; %>
          <% SendMail.sendMail(user,email, title, bookUrl); %>
      </p>
  </ul>
</body>
</html>
