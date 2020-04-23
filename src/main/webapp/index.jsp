<%@ page import="dk.kb.dod.GetRecord" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.FileInputStream" %>
<%@ page import = "java.util.ResourceBundle" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Start DoD</title>
</head>
<body>
Id is : 99123244163205763
  <form action="finish.jsp" method="post">
    <label for="bookId">Bog ID:</label>
    <input type="text" id="bookId" name="bookId" value=""><br>
    <label for="bookUrl">Bog Link:</label>
    <input type="text" id="bookUrl" name="bookUrl" value=""><br>
    <input type="submit" value="Send">
  </form>

  <br>
<%
    Properties dodpro = new Properties();
    try {
        dodpro.load(new FileInputStream("dod.properties"));
    } catch (IOException e) {
        e.printStackTrace();
    }
    String apikey = dodpro.getProperty("alma.apikey");
    %>
    apikey is: <%= apikey %>

</body>
</html>
