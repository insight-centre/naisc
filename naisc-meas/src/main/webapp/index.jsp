<%@ page import="org.insightcentre.uld.naisc.meas.*" %>
<%@ page import="org.insightcentre.uld.naisc.main.Configuration" %>
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
                <h1><img src= "<%= System.getProperties().getProperty("base.url", "")  %>/imgs/logo.png" height="90px"/><br>Meas - The Naisc Evaluation and Analysis Suite</h1>
            </div>
            <div class="row" v-if="activeRuns.length > 0 ">
                <h3>Active runs</h3>
            </div>
            <div class="row" v-if="activeRuns.length >0">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <td class="icon-table-col"></td>
                            <td>Run Identifier</td>
                            <td>Configuration</td>
                            <td>Dataset</td>
                            <td>Stage</td>
                            <td>Status</td>
                            <td class="icon-table-col"></td>
                        </tr>
                    </thead>
                    <tr v-for="run in activeRuns">
                        <td v-if="run.active"><i class="fa fa-spinner fa-spin" style="font-size:24px"></i></td>
                        <td v-if="!run.active"><i class="fas fa-exclamation-triangle"></i></td>
                        <td>{{run.identifier}}</td>
                        <td>{{run.configName}}</td>
                        <td>{{run.datasetName}}</td>
                        <td>{{run.stage}}</td>
                        <td>{{run.status}}</td>
                        <td><button class="btn btn-danger" v-on:click="abortRun(run.identifier)" title="Abort this run"><i class="far fa-hand-paper"></i></button></td>
                    </tr>
                </table>
            </div>
            <div class="row">
                <h3>New run</h3>
            </div>
            <div class="row">
                <form style="width:100%;padding-bottom:100px;">
                    <div class="form-group">
                        <label for="runName">Identifier</label>
                        <input type="text" class="form-control" v-model="identifier" placeholder="identifier" pattern="[A-Za-z][A-Za-z0-9_\-]*">
                    </div>
                    <div class="form-group">
                        <label for="dataset">Dataset</label>
                        <select class="form-control" v-model="datasetName">
                            <option v-for="dataset in datasetNames" v-bind:value="dataset">{{dataset}}</option>
                        </select>
                        <button type="button" class="btn btn-user btn-primary float-right" data-toggle="modal" data-target="#addDataset">
                            <i class="fas fa-upload"></i> Add dataset
                        </button>
                        <button type="button" class="btn btn-user btn-primary float-right" data-toggle="modal" data-target="#downloadDataset" style="margin-right:6px;">
                            <i class="fas fa-cloud-download-alt"></i> Get more datasets
                        </button>
                    </div>
                    <div class="form-group">
                        <label for="configuration">Configuration</label>
                        <select class="form-control" v-model="configName" @change="setConfig()">
                            <option v-for="(c,config) in configs" v-bind:value="config">{{config}}<span v-if="c.description"> - {{c.description}}</span></option>
                        </select>
                        <button type="button" class="btn btn-user btn-primary float-right" data-toggle="modal" data-target="#addConfig">
                            <i class="fas fa-plus-circle"></i>New Configuration</button>
                        <button type="button" class="btn btn-user btn-primary float-right" data-toggle="modal" data-target="#configure" v-show="configName">
                            <i class="fas fa-edit"></i>Edit Configuration</button>
                    </div>
                    <button class="btn btn-user btn-success" type="button" v-on:click.prevent="startRun()"><i class="fas fa-play"></i> Start Run</button>
                    <button class="btn btn-user btn-success" type="button" v-on:click.prevent="train()"><i class="fas fa-dumbbell"></i> Train Model</button>
                    <button class="btn btn-user btn-success" type="button" v-on:click.prevent="crossfold()"><i class="fas fa-flask"></i> Cross-fold Evaluation</button>
                 </form>
                 <div class="modal fade" id="configure" tabindex="-1" role="dialog" aria-labelledby="configModalLabel" aria-hidden="true">
                    <div class="modal-dialog modal-wide" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="configModalLabel">Configuration</h5>
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                            </div>
                            <div class="modal-body">
                                <div class="form-group">
                                    <label for="configName">Configuration Name</label>
                                    <input type="text" class="form-control" id="configNameInput" v-model="configName">
                                </div>
                                <div v-if="configName" id="configMain">
                                    <%= Java2Vue.java2vue(Configuration.class) %>
                                </div>
                                {{config}}
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-dismiss="modal" v-on:click="saveConfig">Save</button>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal fade" id="addDataset" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel" aria-hidden="true">
                    <div class="modal-dialog" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="exampleModalLabel">Upload a dataset</h5>
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                            </div>
                            <form action="<%= System.getProperties().getProperty("base.url", "")  %>/upload_dataset" method="post" enctype="multipart/form-data" id="addDatasetForm">
                                <div class="modal-body">
                                    <div class="form-group">
                                        <label for="datasetName">Name</label>
                                        <input type="text" class="form-control" name="name" id="datasetName">
                                    </div>
                                    <div class="form-group">
                                        <label for="leftUpload">Source Dataset</label>
                                        <input type="file" class="form-control" name="left" id="leftUpload">
                                    </div>
                                    <div class="form-group">
                                        <label for="rightUpload">Target Dataset</label>
                                        <input type="file" class="form-control" name="right" id="rightUpload">
                                    </div>
                                    <div class="form-group">
                                        <label for="alignUpload">Gold standard alignments (optional)</label>
                                        <input type="file" class="form-control" name="align" id="alignUpload">
                                    </div>

                                </div>
                                <div class="modal-footer">
                                    <button type="submit" class="btn btn-secondary" v-on:click.prevent="submitDataset()">Submit</button>
                                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                                </div>
                            <form>
                        </div>
                    </div>
                </div>


                <div class="modal fade" id="downloadDataset" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel" aria-hidden="true">
                    <div class="modal-dialog" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="exampleModalLabel">Download a dataset</h5>
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                            </div>
                            
                            <form action="<%= System.getProperties().getProperty("base.url", "")  %>/download_dataset" enctype="multipart/form-data" id="downloadDatasetForm">
                                <div class="modal-body">
                                    The list of available datasets is on the <a href="http://server1.nlp.insight-centre.org/naisc-datasets/" target="_blank">NUIG Server</a>
                                    <div class="form-group">
                                        <label for="datasetName">Name</label>
                                        <select class="form-control" name="name" id="downloadDatasetName">
                                            <option v-for="dataset in availableDatasets" v-bind:value="dataset">{{dataset}}</option>
                                        </select>
                                    </div>

                                </div>
                                <div class="modal-footer">
                                    <button type="submit" class="btn btn-secondary" v-on:click.prevent="downloadDataset()">Submit</button>
                                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                                </div>
                            <form>
                        </div>
                    </div>
                </div>

                <div class="modal fade" id="addConfig" tabindex="-1" role="dialog" aria-labelledby="addConfigLabel" aria-hidden="true">
                    <div class="modal-dialog" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="addConfigLabel">Add a config</h5>
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                            </div>
                            <form>
                                <div class="modal-body">
                                    <div class="form-group">
                                        <label for="datasetName">Name</label>
                                        <input type="text" class="form-control" name="name" id="newConfigName" pattern="[A-Za-z][A-Za-z0-9]*">
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button type="submit" class="btn btn-secondary" data-dismiss="modal" v-on:click.prevent="newConfig()">Submit</button>
                                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                <div class="modal fade" id="error" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel" aria-hidden="true">
                    <div class="modal-dialog" role="document" style="min-width:90%;">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Operation failed</h5>
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                            </div>
                            <div id="exceptionText"></div>
                        </div>
                    </div>
                </div>

                <div class="modal fade" id="messagesModal" tabindex="-1" role="dialog" aria-labelledby="messagesModelLabel" aria-hidden="true">
                    <div class="modal-dialog modal-wide" role="document">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="messagesModelLabel">Messages</h5>
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                            </div>
                            <div class="modal-body">
                                <table v-if="messages.length > 0" style="width:100%;">
                                    <tr>
                                        <th class="icon-table-col"></th>
                                        <th>Stage</th>
                                        <th>Message</th>
                                    </tr>
                                    <tr v-for="message in messages">
                                        <td>
                                            <i class="fas fa-car-crash" v-if="message.level == 'CRITICAL'"></i>
                                            <i class="fas fa-exclamation-triangle" v-if="message.level == 'WARNING'"></i>
                                            <i class="fas fa-info" v-if="message.level == 'INFO'"></i>
                                        </td>
                                        <td>{{message.stage}}</td>
                                        <td>{{message.message}}</td>
                                    </tr>
                                </table>
                                <div v-if="messages.length == 0">
                                    <i>No messages!</i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>          
            </div>
            <div class="row">
                <h3>Previous runs</h3>
            </div>
            <div class="row">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <td>Run Identifier</td>
                            <td>Configuration</td>
                            <td>Dataset</td>
                            <td>Precision</td>
                            <td>Recall</td>
                            <td>F-Measure</td>
                            <td>Time</td>
                            <td class="icon-table-col"></td>
                            <td class="icon-table-col"></td>
                            <td class="icon-table-col"></td>
                        </tr>
                    </thead>
                    <tr v-for="run in runs">
                        <td><a v-bind:href="'results.jsp?id=' + run.identifier" v-if="!run.isTrain">{{run.identifier}}</a>
                            <span v-if="run.isTrain">{{run.identifier}}</span></td>
                        <td>{{run.configName}}</td>
                        <td>{{run.datasetName}}</td>
                        <td v-if="run.precision >= 0">{{(run.precision*100).toFixed(2)}}%</td>
                        <td v-if="run.precision < 0 && !run.isTrain">n/a</td>
                        <td v-if="run.recall >= 0">{{(run.recall*100).toFixed(2)}}%</td>
                        <td v-if="run.recall < 0 && !run.isTrain">n/a</td>
                        <td v-if="run.fmeasure >= 0">{{(run.fmeasure*100).toFixed(2)}}%</td>
                        <td v-if="run.fmeasure < 0 && !run.isTrain">n/a</td>
                        <td v-if="run.isTrain">Train</td>
                        <td v-if="run.isTrain">Train</td>
                        <td v-if="run.isTrain">Train</td>
                        <!--<td v-if="run.correlation >= -1">{{run.correlation.toFixed(3)}}</td>
                        <td v-if="run.correlation < -1">n/a</td>-->
                        <td>{{(run.time / 1000).toFixed(3)}}s</td>
                        <td><a class="btn btn-info" v-bind:href="'results.jsp?id=' + run.identifier" v-if="!run.isTrain" style="font-weight:bold;">View Results</a></td>
                        <td><a class="btn btn-info" v-on:click="showMessages(run.identifier)" title="Show messages"><i class="fa fa-info" aria-hidden="true"></i></button></td>
                        <td><button class="btn btn-danger" v-on:click="delRun(run.identifier)" title="Delete this run"><i class="fa fa-trash" aria-hidden="true"></i></button></td>
                    </tr>
                </table>
            </div>
        </div>

    <script
  src="<%= System.getProperties().getProperty("base.url", "")  %>/js/jquery-3.3.1.min.js"
  integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8="
  crossorigin="anonymous"></script>
    <script src="<%= System.getProperties().getProperty("base.url", "")  %>/js/popper.min.js" integrity="sha384-ZMP7rVo3mIykV+2+9J3UJ46jBk0WLaUAdn689aCwoqbBJiSnjAK/l8WvCWPIPm49" crossorigin="anonymous"></script>
    <script src="<%= System.getProperties().getProperty("base.url", "")  %>/js/bootstrap.min.js" integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy" crossorigin="anonymous"></script>
    <script src="<%= System.getProperties().getProperty("base.url", "")  %>/js/vue.js"></script>

    <script>var data = <%= Meas.json() %>;</script>
    <script>
function flatten_config(config) {
    var obj = {};
    _flatten_config(config, obj, "");
    return obj;
}

function _flatten_config(config, obj, path) {
    for (var key in config) {
        var newpath = "";
        if(path) {
            newpath = path + "__" + key;
        } else {
            newpath = key;
        }
        if(Array.isArray(config[key])) {
            var arr = [];
            for(var i = 0; i < config[key].length; i++) {
                if(typeof config[key][i] === "object") {
                    var obj2 = {};
                    _flatten_config(config[key][i], obj2, "");                
                    arr.push(obj2);
                } else {
                    arr.push(config[key][i]);
                }
            }
            obj[newpath] = arr;
        } else if((typeof config[key]) === "object") {
              
            _flatten_config(config[key], obj, newpath);
        } else {
            obj[newpath] = config[key];
        }
    }
}
function unflatten_config(config) {
    if(typeof config !== 'object') {
        return config;
    }
    var root = {};
    for(var key in config) {
        var obj = root;
        var paths = key.split("__");
        for(var i = 0; i < paths.length - 1; i++) {
            if(paths[i] in obj) {
                obj = obj[paths[i]];
            } else {
                var o2 = {};
                obj[paths[i]] = o2;
                obj = o2;
            }
        }
        var end = paths[paths.length - 1];
        if(Array.isArray(config[key])) {
            obj[end] = [];
            for(var i = 0; i < config[key].length; i++) {
                obj[end].push(unflatten_config(config[key][i]));
            }
        } else {
            obj[end] = config[key];
        }
    }
    return root;
}
data.polling = null;                
         
var app = new Vue({
  el: '#app',
  data: data,
  methods: {
    setConfig() {
      this.config = flatten_config(this.configs[this.configName]);
    },
    add(elem, elemStr) {
        if(!elem) {
            var copy = JSON.parse(JSON.stringify(this.config));
            copy[elemStr] = [{"name": ""}];
            this.config = copy;
        } else {
            var copy = JSON.parse(JSON.stringify(this.config));
            copy[elemStr].push({"name": ""});
            this.config = copy;
        }
    },
    addStr(pelem, idx, ename, elem) { 
        var copy = JSON.parse(JSON.stringify(pelem));
        if(!copy[idx][ename]) {
            copy[idx][ename] = [""];
        } else {
            copy[idx][ename].push("");
        }
        eval("this." + elem + " = " + JSON.stringify(copy));
    },
    remove(elem, idx) {
        elem.splice(idx, 1);
    },
    changeComp(x, values, idToFind) {
        x.params = values[document.getElementById(idToFind).value];
    },
    newConfig() {
        var configName = document.getElementById("newConfigName").value;
        this.configs[configName] = {};
        this.configName = configName;
        this.config = flatten_config(this.configs[configName]);
    },
    toggleConfig() {
        this.showConfig = !this.showConfig;
    },
    submitDataset() {
        var form = new FormData($("#addDatasetForm")[0]);
        var name = $('#datasetName').val();
        var data = this;
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "") %>/upload_dataset",
            method: "POST",
            data: form,
            processData: false,
            contentType: false,
            success: function(result){ 
                $('#addDataset').modal('hide');
                data.datasetNames.push(name);
                data.datasetName = name;
            },
            error: function(er){ 
                $('#exceptionText').html(er.responseText);
                $('#error').modal('show'); 
            }
        });
    },
    downloadDataset() {
        var form = new FormData($("#downloadDatasetForm")[0]);
        var name = $('#downloadDatasetName').val();
        var data = this;
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/download_dataset?dataset=" + name,
            success: function(result){ 
                $('#downloadDataset').modal('hide');
                data.datasetNames.push(name);
                data.datasetName = name;
            },
            error: function(er) { 
                $('#downloadDataset').modal('hide');
                $('#exceptionText').html(er.responseText);
                $('#error').modal('show');
            }
        });
    },        
    startRun() {
        var configName = this.configName;
        var datasetName = this.datasetName;
        console.log(this.config);
        var data = this;
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/execute/start",
            method: "POST",
            data: JSON.stringify({"config": unflatten_config(this.config), "configName": this.configName, "dataset": this.datasetName, "runId": this.identifier }),
            processData: false,
            success: function(result) {
                var id = result;
                data.activeRuns.push({
                    "configName": configName,
                    "datasetName": datasetName,
                    "identifier": id,
                    "stage": "INITIALIZING",
                    "status": "Submitted",
                    "active": true
                });
            },
            error: function(er) { 
                $('#exceptionText').html(er.responseText);
                $('#error').modal('show');
             }
        });
    },
    train() {
        var configName = this.configName;
        var datasetName = this.datasetName;
        var data = this;
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/execute/train",
            method: "POST",
            data: JSON.stringify({"config": unflatten_config(this.config), "configName": this.configName, "dataset": this.datasetName, "runId": this.identifier }),
            processData: false,
            success: function(result) {
                var id = result;
                data.activeRuns.push({
                    "configName": configName,
                    "datasetName": datasetName,
                    "identifier": id,
                    "stage": "INITIALIZING",
                    "status": "Submitted",
                    "active": true
                });
            },
            error: function(er) { 
                $('#exceptionText').html(er.responseText);
                $('#error').modal('show'); 
            }
        });
    },
    crossfold() {
        var configName = this.configName;
        var datasetName = this.datasetName;
        var data = this;
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/execute/crossfold",
            method: "POST",
            data: JSON.stringify({"config": unflatten_config(this.config), "configName": this.configName, "dataset": this.datasetName, "runId": this.identifier }),
            processData: false,
            success: function(result) {
                var id = result;
                data.activeRuns.push({
                    "configName": configName,
                    "datasetName": datasetName,
                    "identifier": id,
                    "stage": "INITIALIZING",
                    "status": "Submitted",
                    "active": true
                });
            },
            error: function(er) { 
                $('#exceptionText').html(er.responseText);
                $('#error').modal('show');
            }
        });
    },
    pollData() {
        this.polling = setInterval(() => {
            for(i = 0; i < this.activeRuns.length; i++) {
                if(this.activeRuns[i].active) {
                    var elem = this.activeRuns[i];
                    var id = this.activeRuns[i].identifier;
                    jQuery.ajax({
                        url: "<%= System.getProperties().getProperty("base.url", "")  %>/execute/status/" + id,
                        method: "GET",
                        success: function(result) {
                            var data = JSON.parse(result);
                            elem.stage = data.stage;
                            elem.status = data.lastMessage;
                            elem.active = data.stage !== "FAILED" && data.stage !== "COMPLETED";
                            if(data.stage === "COMPLETED") {
                                jQuery.ajax({
                                    url: "<%= System.getProperties().getProperty("base.url", "")  %>/execute/completed/" + id,
                                    method: "GET",
                                    success: function(result2) {
                                        var data2 = JSON.parse(result2);
                                        for(j = 0; j < app.activeRuns.length; j++) {
                                            if(app.activeRuns[j].identifier === id) {
                                                app.activeRuns.splice(j, 1);
                                            }
                                        }
                                        app.runs.splice(0,0,data2);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }, 500);
    },
    abortRun(runId) {
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/execute/kill/" + runId,
            method: "GET",
            success: function(result) {
                for(i = 0; i < app.activeRuns.length; i++) {
                    if(app.activeRuns[i].identifier === runId) {
                        app.activeRuns.splice(i, 1);
                        break;
                    }
                }
            }
        });
    },
    delRun(runId) {
        if(confirm("Are you sure you want to delete this run?")) {
            jQuery.ajax({
                url: "<%= System.getProperties().getProperty("base.url", "")  %>/manage/remove/" + runId,
                method: "GET",
                success: function(result) {
                    for(i = 0; i < app.runs.length; i++) {
                        if(app.runs[i].identifier === runId) {
                            app.runs.splice(i, 1);
                            break;
                        }
                    }
                }
            });
        }
    },
    saveConfig() {
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/manage/save_config/" + this.configName,
            method: "POST",
            data: JSON.stringify(unflatten_config(this.config))
        });
    },
    showMessages(id) {
        jQuery.ajax({
            url: "<%= System.getProperties().getProperty("base.url", "")  %>/manage/messages?id=" + id,
            success: function(result) {
                app.messages = result;
                console.log(JSON.stringify(result));
                $('#messagesModal').modal('show');
            },
            failure: function(result) {
                console.log("failed to get messages");
            }
        });
    }       
  },
  beforeDestroy() {
    clearInterval(this.polling);
  },
  created() {
    this.pollData();
  }
})
</script>
  </body>
</html>