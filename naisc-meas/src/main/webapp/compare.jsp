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
                    <h1><a href="<%= System.getProperties().getProperty("base.url", "")  %>/"><img src= "<%= System.getProperties().getProperty("base.url", "")  %>/imgs/logo.png" height="90px"/></a><br>Comparing <%= request.getParameter("first") %> and <%= request.getParameter("second")%></h1>

                <h3 class="results_title">Differences</h3>
                <table class="table table-striped">
                    <tr>
                        <th>Subject</th>
                        <th>Property</th>
                        <th>Object</th>
                        <th>Scores</th>
                        <th>Evaluation</th>
                    </tr>
                    <tr v-for="(result, idx) in comparison">
                        <td>
                            <a v-bind:href="result.subject">{{displayUrl(result.subject)}}</a>
                        </td>
                        <td>
                            <a v-bind:href="result.subject">{{displayUrl(result.property)}}</a>
                        </td>
                        <td>
                            <a v-bind:href="result.subject">{{displayUrl(result.object)}}</a>
                        </td>
                        <td>
                            <b>First:</b> {{result.firstScore}}<br/>
                            <b>Second:</b> {{result.secondScore}}
                        </td>
                        <td>
                            <b>First:</b> {{result.firstValid}}<br/>
                            <b>Second:</b> {{result.secondValid}}
                        </td>
                    </tr>
                </table>
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
   "comparison":<%= Meas.loadComparison(request.getParameter("first"), request.getParameter("second")) %>
   };
var app = new Vue({
  el: '#app',
  data: data,
  methods: {
    displayUrl(url) {
        if(url == null) { return "null"; }
        var hashIndex = url.indexOf('#');
        if(hashIndex > 0) {
            return url.substring(hashIndex + 1);
        }
        var slashIndex = url.lastIndexOf('/');
        if(slashIndex > 0) {
            return url.substring(slashIndex + 1);
        }
        return url;
    }
  }
});
    </script>
  </body>
</html>

