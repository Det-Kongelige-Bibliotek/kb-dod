<%@ page import="dk.kb.dod.facade.DodFacade" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="org.springframework.web.servlet.support.RequestContextUtils" %>


<%
    ApplicationContext ac = RequestContextUtils.findWebApplicationContext(request);
    DodFacade facade = ac.getBean(DodFacade.class);
%>


<!DOCTYPE html>
<html lang="en">
<head>
    <%--From https://stackoverflow.com/a/2206865--%>
    <base href="${pageContext.request.contextPath}/"/>
    <meta charset="UTF-8">
    <title>Start DoD</title>

    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <link rel="stylesheet" href="https://unpkg.com/bootstrap-table@1.16.0/dist/bootstrap-table.min.css">

    <!-- Bootstrap CSS -->
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css"
          integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh" crossorigin="anonymous">


    <link href="https://unpkg.com/bootstrap-table@1.16.0/dist/extensions/group-by-v2/bootstrap-table-group-by.css"
          rel="stylesheet">

<body>
Id is : 99123244163205763
<form action="finish.jsp" method="post">
    <label for="mmsID">Bog ID:</label>
    <input type="text" id="mmsID" name="mmsID" value=""><br>
    <label for="bookUrl">Bog Link:</label>
    <input type="text" id="bookUrl" name="bookUrl" value=""><br>
    <input type="submit" value="Send">
</form>

<br>
<%
    String almaEnv = facade.getAlmaEnvType();
    String almaHost = facade.getAlmaHost();
%>
<p>Alma host is: <%= almaHost %></p>
<p>Alma env is: <%= almaEnv %></p>


<script src="https://code.jquery.com/jquery-3.4.1.slim.min.js"
        integrity="sha384-J6qa4849blE2+poT4WnyKhv5vZF5SrPo0iEjwBvKU7imGFAV0wwj1yYfoRSJoZ+n"
        crossorigin="anonymous"></script>

<script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js"
        integrity="sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo"
        crossorigin="anonymous"></script>

<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js"
        integrity="sha384-wfSDF2E50Y2D1uUdj0O3uMBJnjuUD4Ih7YwaYd1iqfktj0Uod8GCExl3Og8ifwB6"
        crossorigin="anonymous"></script>

<script src="https://unpkg.com/bootstrap-table@1.16.0/dist/bootstrap-table.min.js"></script>

<script
    src="https://unpkg.com/bootstrap-table@1.16.0/dist/extensions/group-by-v2/bootstrap-table-group-by.min.js"></script>
</body>
</html>
