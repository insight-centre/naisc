<%@ page import="org.insightcentre.uld.naisc.meas.*" %>
<%@ page import="org.insightcentre.uld.naisc.meas.execution.*" %>
<%@ page import="org.insightcentre.uld.naisc.main.Configuration" %>
<% int limit = 50; %>
<!doctype html>
<html lang="en">
  <head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <link href="https://fonts.googleapis.com/css?family=Patua+One|Unica+One|Rokkitt" rel="stylesheet">

    <link rel="stylesheet" href="<%= System.getProperties().getProperty("base.url", "")  %>/css/bootstrap.min.css">
    <link rel="stylesheet" href="<%= System.getProperties().getProperty("base.url", "")  %>/css/all.min.css">
    <link rel="stylesheet" href="<%= System.getProperties().getProperty("base.url", "")  %>/css/meas.css">

    <title>Meas - The Naisc Evaluation and Analysis Suite</title>
    </head>
    <body>
        <div class="container" id="app">
            <div class="row">
                <h1><a href="<%= System.getProperties().getProperty("base.url", "")  %>/"><img src= "<%= System.getProperties().getProperty("base.url", "")  %>/imgs/logo.png" height="90px"/></a><br>Results for <%= request.getParameter("id") %></h1>
            </div>
        </div>
        <script src="<%= System.getProperties().getProperty("base.url", "")  %>/js/jquery-3.3.1.min.js"
                integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8="
                crossorigin="anonymous"></script>
    <script src="<%= System.getProperties().getProperty("base.url", "")  %>/js/popper.min.js" integrity="sha384-ZMP7rVo3mIykV+2+9J3UJ46jBk0WLaUAdn689aCwoqbBJiSnjAK/l8WvCWPIPm49" crossorigin="anonymous"></script>
    <script src="<%= System.getProperties().getProperty("base.url", "")  %>/js/bootstrap.min.js" integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy" crossorigin="anonymous"></script>
    <script src="<%= System.getProperties().getProperty("base.url", "")  %>/js/vue.js"></script>
    <script>
var data = {
   "first":<%= Meas.loadRunResult(request.getParameter("first"), request.getParameter("firstOffset") == null ? 0  : Integer.parseInt(request.getParameter("firstOffset")), limit) %>,
   "second":<%= Meas.loadRunResult(request.getParameter("second"), request.getParameter("secondOffset") == null ? 0  : Integer.parseInt(request.getParameter("secondOffset")), limit) %>
   };

data.totalResults = <%= Execution.noResults(request.getParameter("id")) %>;
data.offset = <%= request.getParameter("offset") == null ? 0  : Integer.parseInt(request.getParameter("offset")) %>;
data.tp = <%= Execution.truePositives(request.getParameter("id")) %>;
data.fp = <%= Execution.falsePositives(request.getParameter("id")) %>;
data.fn = <%= Execution.falseNegatives(request.getParameter("id")) %>;
data.currentElem = "";
data.elems = new Set();
data.left = true;
data.updateIdx = 0;
    </script>
  </body>
</html>

