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
            <div class="row">
                <div class="col percentage percentage-bordered">
                    <div class="percentage-large">{{precision()}}</div>
                    <div class="percentage-title">Precision</div>
                </div>
                <div class="col percentage percentage-bordered">
                    <div class="percentage-large">{{recall()}}</div>
                    <div class="percentage-title">Recall</div>
                </div>
                <div class="col percentage">
                    <div class="percentage-large">{{fmeasure()}}</div>
                    <div class="percentage-title">F-Measure</div>
                </div>
            </div>
            <div class="row" v-if="le > 0">
                <div class="col percentage percentage-bordered">
                    <div class="percentage-large">{{linkPrecision()}}</div>
                    <div class="percentage-title">Link Precision</div>
                </div>
                <div class="col percentage percentage-bordered">
                    <div class="percentage-large">{{linkRecall()}}</div>
                    <div class="percentage-title">Link Recall</div>
                </div>
                <div class="col percentage">
                    <div class="percentage-large">{{linkFmeasure()}}</div>
                    <div class="percentage-title">Link F-Measure</div>
                </div>
            </div>

            <div class="row">
                <div class="col">
                <center>
                    <a href="<%= System.getProperties().getProperty("base.url", "")  %>/manage/download_all/<%= request.getParameter("id") %>"><button type="button" class="btn btn-user btn-info" data-toggle="tooltip" data-placement="top" title="Downloads links evaluated as 'Yes' or 'No'"><i class="fas fa-download"></i> Download output links</button></a>
                    <a href="<%= System.getProperties().getProperty("base.url", "")  %>/manage/download_valid/<%= request.getParameter("id") %>"><button type="button" class="btn btn-user btn-info" data-toggle="tooltip" data-placement="top" title="Downloads links evaluated as 'Yes' and new links"><i class="fas fa-check-double"></i> Download validated</button></a>
                    <button type="button" class="btn btn-user btn-info" data-toggle="tooltip" data-placement="top" title="Compare this run with another run" v-on:click="compare()"><i class="fas fa-not-equal"></i> Compare Results</button>
                </center>
                </div>
            </div>
            <div class="row">
                <h3 class="results_title">Results {{offset+1}}-{{Math.min(offset+<%=limit%>,totalResults)}} of {{totalResults}}</h3>
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <td>Left Identifier</td>
                            <td>Relation</td>
                            <td>Right Identifier</td>
                            <td>Score</td>
                            <td>Evaluation</td>
                         </tr>
                    </thead>
                    <tr v-for="(result,idx) in results" v-bind:class="{'valid-yes':result.valid === 'yes','valid-no':result.valid === 'no' || result.valid === 'bad_link','valid-unknown':result.valid === 'unknown','valid-novel':result.valid === 'novel'}">
                        <td>
                            <span v-if="result.leftRoot && result.leftRoot !== result.subject"><a v-bind:href="result.leftRoot" class="root">{{displayUrl(result.leftRoot)}}</a><br/></span>
                            <div v-for="(lp, lpidx) in result.leftPath">
                                <span v-if="lp"><a v-bind:href="lp" class="treeLink" v-bind:style="{ marginLeft: (lpidx*10 + 10) + 'px' }">{{displayUrl(lp)}}</a><br/></span>
                            </div>
                            <a v-bind:href="result.subject" v-bind:style="{ marginLeft: (result.leftPath.length*10 + 20) + 'px' }">{{displayUrl(result.subject)}}</a>
                            <button type="button" class="btn btn-info" title="Change this entity" v-if="result.valid==='no' || result.valid === 'bad_link'" v-on:click.prevent="changeLeft(idx,result.subject)">
                                <i class="fas fa-wrench"></i></button>
                            <div v-for="(l, lensid) in result.lens" v-bind:style="{ marginLeft: (result.leftPath.length*10 + 20) + 'px' }">
                                <span class="lens-id">{{lensid}}:</span> <span class="lens-content">{{l.string1}}</span> <span class="lens-language">{{l.lang1}}</span>
                            </div>
                        </td>
                        <td><a v-bind:href="result.property">{{displayUrl(result.property)}}</a></td>
                        <td>
                            <span v-if="result.rightRoot && result.rightRoot !== result.object"><a v-bind:href="result.rightRoot" class="root">{{displayUrl(result.rightRoot)}}</a><br/></span>
                            <div v-for="(lp, lpidx) in result.rightPath">
                                <span v-if="lp"><a v-bind:href="lp" class="treeLink" v-bind:style="{ marginLeft: (lpidx*10 + 10) + 'px' }">{{displayUrl(lp)}}</a><br/></span>
                            </div>
                            <a v-bind:href="result.object" v-bind:style="{ marginLeft: (result.rightPath.length*10 + 20) + 'px' }">{{displayUrl(result.object)}}</a>
                            <button type="button" class="btn btn-info" title="Change this entity" v-if="result.valid==='no' || result.valid === 'bad_link'" v-on:click.prevent="changeRight(idx,result.object)">
                                <i class="fas fa-wrench"></i></button>
                            <div v-for="(l, lensid) in result.lens" v-bind:style="{ marginLeft: (result.rightPath.length*10 + 20) + 'px' }">
                                <span class="lens-id">{{lensid}}:</span> <span class="lens-content">{{l.string2}}</span> <span class="lens-language">{{l.lang2}}</span>
                            </div>
                        </td>
                        <td>
                            <div class="progress position-relative">
                                <div class="progress-bar progress-bar-striped" role="progressbar" 
                                      v-bind:style="{'width': result.score*100 +'%'}" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100"></div>
                                <span class="justify-content-center d-flex position-absolute w-100">{{(result.score).toFixed(2)}}</span>

                            </div>
                        </td>
                        <td>
                            <div class="btn-group" role="group" v-if="result.valid !== 'novel'">
                                <button type="button" class="btn" v-bind:class="{'btn-primary': result.valid === 'yes', 'btn-secondary':result.valid !== 'yes'}" v-on:click="updateLink(idx,'yes')">Yes</button>
                                <button type="button" class="btn" v-bind:class="{'btn-primary': result.valid === 'no' || result.valid === 'bad_link', 'btn-secondary':result.valid !== 'no' && result.valid !== 'bad_link'}" v-on:click="updateLink(idx,'no')">No</button>
                                <button type="button" class="btn" v-bind:class="{'btn-primary': result.valid === 'unknown', 'btn-secondary':result.valid !== 'unknown'}" v-on:click="updateLink(idx,'unknown')">?</button>
                            </div>
                            <div v-if="result.valid === 'novel'">
                                <button type="button" class="btn btn-danger" v-on:click.prevent="removeLink(idx)" title="Remove this link"><i class="fa fa-trash" aria-hidden="true"></i></button>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td>
                        <button type="button" class="btn btn-dark" v-bind:class="{disabled: offset <= 0}" v-on:click="prevResults()">&laquo; Previous <%=limit%> Links</button>
                    </td>
                    <td></td>
                    <td></td>
                    <td></td>
                        <td>
                        <button type="button" class="btn btn-dark" v-bind:class="{disabled: offset + <%=limit%> > totalResults}" v-on:click="nextResults()">Next <%=limit%> Links &raquo;</button>
                        </td>
                    </tr>
                </table>
                <div class="row spaced-buttons">
                <button type="button" class="btn btn-user btn-success" v-on:click.prevent="rerun()" data-toggle="tooltip" data-placement="top" title="Rerun the system including the correct links"><i class="fas fa-redo"></i> Rerun</button>
                <button type="button" class="btn btn-user btn-success" v-on:click.prevent="retrain()" data-toggle="tooltip" data-placement="top" title="Retrain the model using the marked data"><i class="fas fa-dumbbell"></i> Retrain</button>
                <a href="<%= System.getProperties().getProperty("base.url", "")  %>/manage/download_all/<%= request.getParameter("id") %>"><button type="button" class="btn btn-user btn-info" data-toggle="tooltip" data-placement="top" title="Downloads links evaluated as 'Yes' or 'No'"><i class="fas fa-download"></i> Download output links</button></a>
                <a href="<%= System.getProperties().getProperty("base.url", "")  %>/manage/download_valid/<%= request.getParameter("id") %>"><button type="button" class="btn btn-user btn-info" data-toggle="tooltip" data-placement="top" title="Downloads links evaluated as 'Yes' and new links"><i class="fas fa-check-double"></i> Download validated</button></a>
                </div>

                <div class="modal fade" id="updateElement" tabindex="-1" role="dialog"  aria-hidden="true">
                    <div class="modal-dialog" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="updateElementLabel">Change entity</h5>
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                            </div>
                            <div class="modal-body">
                            <form>
                                <div class="form-group">
                                    <label for="updateElementElem">Element</label>
                                    
                                    <select class="form-control" v-model="currentElem">
                                        <option v-for="elem in elems" v-bind:value="elem.id">{{elem.display}}</option>
                                    </select>
                                </div>
                                <div class="modal-footer" style="text-align:center;">
                                    <button type="submit" class="btn btn-secondary" data-dismiss="modal" v-on:click.prevent="addNew()">Update</button>
                                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                                </div>
                            </form>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="modal fade" id="compareModal" tabindex="-1" role="dialog" aria-labelledby="compareModalLabel" aria-hidden="true">
                    <div class="modal-dialog" role="document" style="min-width:90%;">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="compareModalLabel">Compare Results</h5>
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                            </div>
                            <div class="modal-body">
                                <form method="get" action="<%= System.getProperties().getProperty("base.url", "")  %>/compare.jsp">
                                    <input type="hidden" value="<%= request.getParameter("id") %>" name="first"/>
                                    <div class="form-group">
                                        <label for="compareTarget">Compare to:</label>
                                        <select class="form-control" name="second">
                                            <option v-for="elem in otherDatasets" v-bind:value="elem.identifier">{{elem.identifier}}</option>
                                        </select>
                                    </div>
                                    <div class="modal-footer" style="text-align:center;">
                                        <button type="submit" class="btn btn-success">Compare</button>
                                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
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
    "results":<%= Meas.loadRunResult(request.getParameter("id"), request.getParameter("offset") == null ? 0  : Integer.parseInt(request.getParameter("offset")), limit) %>,
    "otherDatasets": <%= Meas.runsJson() %>
    };

data.totalResults = <%= Execution.noResults(request.getParameter("id")) %>;
data.offset = <%= request.getParameter("offset") == null ? 0  : Integer.parseInt(request.getParameter("offset")) %>;
data.tp = <%= Execution.truePositives(request.getParameter("id")) %>;
data.fp = <%= Execution.falsePositives(request.getParameter("id")) %>;
data.le = <%= Execution.linkErrors(request.getParameter("id")) %>;
data.fn = <%= Execution.falseNegatives(request.getParameter("id")) %>;
data.currentElem = "";
data.elems = new Set();
data.left = true;
data.updateIdx = 0;

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
    },
    updateLink(idx, value) {
        if(this.results[idx].valid === 'yes') {
            if(value === 'no' || value === 'bad_link') {
                this.tp -= 1;
                this.fp += 1;
            } else if(value === 'unknown') {
                this.tp -= 1;
            }
        } else if (this.results[idx].valid === 'no' || this.results[idx].valid  === 'bad_link') {
            if(value === 'yes') {
                this.tp += 1;
                this.fp -= 1;
            } else if(value === 'unknown') {
                this.fp -= 1;
            }
        } else if(this.results[idx].valid === 'unknown') {
            if(value === 'yes') {
                this.tp += 1;
            } else if(value === 'no' || value === 'bad_link') {
                this.fp += 1;
            }
        }
        this.results[idx].valid = value;
        var remoteIdx = this.results[idx].idx;
        jQuery.ajax({
            "url": "<%= System.getProperties().getProperty("base.url", "")  %>/manage/update/<%= request.getParameter("id") %>",
            data: JSON.stringify({
                "idx": remoteIdx,
                "valid": value,
                "data": {"identifier":"<%= request.getParameter("id") %>", "precision": this.precnum(), "recall": this.recnum(), "fmeasure": this.fmnum()}}),
            method: "POST",
            processData: false,
            error: function(er){ document.write(er.responseText); }
        });
    },
    precision() {
        if(this.tp > 0 || this.fp > 0) {
            return (100 *this.tp / (this.tp + this.fp)).toFixed(1) + "%";
        } else {
            return "n/a";
        }
    },
    linkPrecision() {
        if(this.tp > 0 || this.fp > 0) {
            return (100 * (this.tp + this.le) / (this.tp + this.fp)).toFixed(1) + "%";
        } else {
            return "n/a";
        }
    },
    recall() {
        if(this.tp > 0 || this.fn > 0) {
            return (100 *this.tp / (this.tp + this.fn)).toFixed(1) + "%";
        } else {
            return "n/a";
        }
    },
    linkRecall() {
        if(this.tp > 0 || this.fn > 0) {
            return (100 * (this.tp + this.le) / (this.tp + this.fn)).toFixed(1) + "%";
        } else {
            return "n/a";
        }
    },

    fmeasure() {
        if(this.tp > 0 || this.fn > 0 || this.fp > 0) {
            return (200 *this.tp / (2*this.tp + this.fn + this.fp)).toFixed(1) + "%";
        } else {
            return "n/a";
        }
    },
    linkFmeasure() {
        if(this.tp > 0 || this.fn > 0 || this.fp > 0) {
            return (200 * (this.tp + this.le) / (2*this.tp + this.fn + this.fp)).toFixed(1) + "%";
        } else {
            return "n/a";
        }
    },
    precnum() {
        if(this.tp > 0 || this.fp > 0) {
            return this.tp / (this.tp + this.fp);
        } else {
            return -1.0;
        }
    },
    recnum() {
        if(this.tp > 0 || this.fn > 0) {
            return this.tp / (this.tp + this.fn);
        } else {
            return -1.0;
        }
    },
    fmnum() {
        if(this.tp > 0 || this.fn > 0 || this.fp > 0) {
            return 2 * this.tp / (2*this.tp + this.fn + this.fp)
        } else {
            return -1.0;
        }
    },
    changeRight(idx, currentValue) {
        var self = this;
        jQuery.ajax({
            url: '<%= System.getProperties().getProperty("base.url", "")  %>/manage/alternatives?id=<%=request.getParameter("id")%>&left=' + encodeURIComponent(self.results[idx].subject),
            success: function(result) {
                self.elems = [];
                for(var i = 0; i < result.length; i++) {
                    var display = "";
                    for(var key in result[i]["_2"]) {
                        display += result[i]["_2"][key] + " (" + key + ")";
                    }
                    var e = {"id": result[i]["_1"], "display": display};
                    self.elems.push(e);
                }
                self.currentElem = currentValue;
                self.left = false;
                self.updateIdx = idx;
                $('#updateElement').modal('show');
            }
        });
    },
    changeLeft(idx, currentValue) {
        var remoteIdx = this.results[idx].idx;
        var self = this;
        jQuery.ajax({
            url: '<%= System.getProperties().getProperty("base.url", "")  %>/manage/alternatives?id=<%=request.getParameter("id")%>&right=' + encodeURIComponent(self.results[idx].object),
            success: function(result) {
                self.elems = [];
                for(var i = 0; i < result.length; i++) {
                    var display = "";
                    for(var key in result[i]["_2"]) {
                        display += result[i]["_2"][key] + " (" + key + ")";
                    }
                    var e = {"id": result[i]["_1"], "display": display};
                    self.elems.push(e);
                }
                self.currentElem = currentValue;
                self.left = true;
                self.updateIdx = idx;
                $('#updateElement').modal('show');
            }
        });
    },
    addNew() {
        var newRow = JSON.parse(JSON.stringify(this.results[this.updateIdx]));
        newRow.lens = {};
        newRow.valid = 'novel';
        var change;
        if(this.left) {
            change = (newRow.subject != this.currentElem);
            newRow.subject = this.currentElem;
        } else {
            change = (newRow.object != this.currentElem);
            newRow.object = this.currentElem;
        }
        if(change) {
            this.results.splice(this.updateIdx + 1, 0, newRow);
            this.fn++;
        }
        $('#updateElement').modal('hide');
        var remoteIdx = this.results[this.updateIdx].idx;
        jQuery.ajax({
            "url": "<%= System.getProperties().getProperty("base.url", "")  %>/manage/add/<%= request.getParameter("id") %>",
            data: JSON.stringify({
                idx: remoteIdx,
                subject: newRow.subject,
                object: newRow.object,
                property: newRow.property,
                data: {"identifier":"<%= request.getParameter("id") %>", "precision": this.precnum(), "recall": this.recnum(), "fmeasure": this.fmnum()}}),
            method: "POST",
            processData: false,
            success: function(er) { newRow.idx = er.data; },
            error: function(er){ document.write(er.responseText); }
        }); 
    },
    removeLink(idx) {
        var remoteIdx = this.results[idx].idx;
        alert(remoteIdx);
        this.results.splice(idx,1);
        this.fn--;
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/manage/remove/<%= request.getParameter("id") %>",
            data: JSON.stringify({
                "idx": remoteIdx,
                "data": {"identifier":"<%= request.getParameter("id") %>", "precision": this.precnum(), "recall": this.recnum(), "fmeasure": this.fmnum()}}),
            method: "POST",
            processData: false,
            error: function(er){ document.write(er.responseText); }
        }); 
    },
    rerun() {
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/execute/rerun/<%= request.getParameter("id") %>",
            success: function() { window.location = "<%= System.getProperties().getProperty("base.url", "")  %>/index.jsp"; },
            error: function(er) { document.write(er.responseText); }
        });
    },
    retrain() {
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/execute/retrain/<%= request.getParameter("id") %>",
            success: function() { window.location = "<%= System.getProperties().getProperty("base.url", "")  %>/index.jsp"; },
            error: function(er) { document.write(er.responseText); }
        });
    },
    prevResults($router) {
        if(this.offset > 0) {
        var self = this;
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/manage/results?id=<%= request.getParameter("id") %>&offset=" + (this.offset-<%=limit%>) + "&limit=<%=limit%>",
            success: function(d) {
                self.results = d;
                self.offset -= <%=limit%>;
                $router.push({ "path": "<%= System.getProperties().getProperty("base.url", "")  %>/results.jsp", "query": { "id": "<%= request.getParameter("id") %>", "offset": offset }});
            }, 
            error: function(er){ document.write(er.responseText); }
        });
        }
    },
    nextResults($router) {
        if(this.offset + <%=limit%> < this.totalResults) {
        var self = this;
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/manage/results?id=<%= request.getParameter("id") %>&offset=" + (this.offset+<%=limit%>) + "&limit=<%=limit%>",
            success: function(d) {
                self.results = d;
                self.offset += <%=limit%>;
                $router.push({ "path": "<%= System.getProperties().getProperty("base.url", "")  %>/results.jsp", "query": { "id": "<%= request.getParameter("id") %>", "offset": offset }});
            }, 
            error: function(er){ document.write(er.responseText); }
        });
        } 
    },
    compare() {
        $('#compareModal').modal('show');
    }
  }
});
$(function () {
  $('[data-toggle="tooltip"]').tooltip()
})
</script>
    </body>
</html>

