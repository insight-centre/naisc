<%@ page import="org.insightcentre.uld.naisc.meas.*" %>
<%@ page import="org.insightcentre.uld.naisc.main.Configuration" %>
<!doctype html>
<html lang="en">
  <head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <link href="https://fonts.googleapis.com/css?family=Patua+One|Unica+One|Rokkitt" rel="stylesheet">

    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/all.min.css">
    <link rel="stylesheet" href="/css/meas.css">

    <title>Meas - The Naisc Evaluation and Analysis Suite</title>
    </head>
    <body>
        <div class="container" id="app">
            <div class="row">
                <h1><a href="/"><img src= "/imgs/logo.png" height="90px"/></a><br>Results for <%= request.getParameter("id") %></h1>
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
            <div class="row">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <td>Left Identifier</td>
                            <td>Relation</td>
                            <td>Right Identifier</td>
                            <td>Score</td>
                            <td>Evaluation</td>
                    </thead>
                    <tr v-for="(result,idx) in results" v-bind:class="{'valid-yes':result.valid === 'yes','valid-no':result.valid === 'no','valid-unknown':result.valid === 'unknown','valid-novel':result.valid === 'novel'}">
                        <td><a v-bind:href="result.subject">{{displayUrl(result.subject)}}</a>
                            <button type="button" class="btn btn-info" title="Change this entity" v-if="result.valid==='no'" v-on:click.prevent="changeLeft(idx,result.subject)">
                                <i class="fas fa-wrench"></i></button>
                            <div v-for="(l, lensid) in result.lens">
                                <span class="lens-id">{{lensid}}:</span> <span class="lens-content">{{l._1}}</span> <span class="lens-language">{{l.lang1}}</span>
                            </div>
                        </td>
                        <td><a v-bind:href="result.property">{{displayUrl(result.property)}}</a></td>
                        <td><a v-bind:href="result.object">{{displayUrl(result.object)}}</a>
                            <button type="button" class="btn btn-info" title="Change this entity" v-if="result.valid==='no'" v-on:click.prevent="changeRight(idx,result.object)">
                                <i class="fas fa-wrench"></i></button>
                            <div v-for="(l, lensid) in result.lens">
                                <span class="lens-id">{{lensid}}:</span> <span class="lens-content">{{l._2}}</span> <span class="lens-language">{{l.lang2}}</span>
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
                                <button type="button" class="btn" v-bind:class="{'btn-primary': result.valid === 'no', 'btn-secondary':result.valid !== 'no'}" v-on:click="updateLink(idx,'no')">No</button>
                                <button type="button" class="btn" v-bind:class="{'btn-primary': result.valid === 'unknown', 'btn-secondary':result.valid !== 'unknown'}" v-on:click="updateLink(idx,'unknown')">?</button>
                            </div>
                            <div v-if="result.valid === 'novel'">
                                <button type="button" class="btn btn-danger" v-on:click.prevent="removeLink(idx)" title="Remove this link"><i class="fa fa-trash" aria-hidden="true"></i></button>
                            </div>
                        </td>
                    </tr>
                </table>
                <div class="row spaced-buttons">
                <button type="button" class="btn btn-success" v-on:click.prevent="save()"><i class="fas fa-save"></i> Save</button>
                <button type="button" class="btn btn-success" v-on:click.prevent="rerun()"><i class="fas fa-redo"></i> Rerun</button>
                <button type="button" class="btn btn-success" v-on:click.prevent="retrain()"><i class="fas fa-dumbbell"></i> Retrain</button>
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
                            <form>
                                <div class="form-group">
                                    <label for="updateElementElem">Element</label>
                                    <select class="form-control" v-model="currentElem">
                                        <option v-for="elem in elems" v-bind:value="elem">{{displayUrl(elem)}}</option>
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
        </div>
    <script src="/js/jquery-3.3.1.min.js"
  integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8="
  crossorigin="anonymous"></script>
    <script src="/js/popper.min.js" integrity="sha384-ZMP7rVo3mIykV+2+9J3UJ46jBk0WLaUAdn689aCwoqbBJiSnjAK/l8WvCWPIPm49" crossorigin="anonymous"></script>
    <script src="/js/bootstrap.min.js" integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy" crossorigin="anonymous"></script>
    <script src="/js/vue.js"></script>
<script>
var data = {"results":<%= Meas.loadRunResult(request.getParameter("id")) %>};

function count(value) {
    var n = 0;
    for(var i = 0; i < data.results.length; i++) {
        if(data.results[i].valid === value) {
            n++;
        }
    }
    return n;
}

data.tp = count('yes');
data.fp = count('no');
data.fn = count('novel');
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
            if(value === 'no') {
                this.tp -= 1;
                this.fp += 1;
            } else if(value === 'unknown') {
                this.tp -= 1;
            }
        } else if (this.results[idx].valid === 'no') {
            if(value === 'yes') {
                this.tp += 1;
                this.fp -= 1;
            } else if(value === 'unknown') {
                this.fp -= 1;
            }
        } else if(this.results[idx].valid === 'unknown') {
            if(value === 'yes') {
                this.tp += 1;
            } else if(value === 'no') {
                this.fp += 1;
            }
        }
        this.results[idx].valid = value;
    },
    precision() {
        if(this.tp > 0 || this.fp > 0) {
            return (100 *this.tp / (this.tp + this.fp)).toFixed(1) + "%";
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
    fmeasure() {
        if(this.tp > 0 || this.fn > 0 || this.fp > 0) {
            return (200 *this.tp / (2*this.tp + this.fn + this.fp)).toFixed(1) + "%";
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
        this.elems = new Set();
        for(var i = 0; i < this.results.length; i++) {
            this.elems.add(this.results[i].object);
        }
        this.elems = Array.from(this.elems);
        this.currentElem = currentValue;
        this.left = false;
        this.updateIdx = idx;
        $('#updateElement').modal('show');
    
    },
    changeLeft(idx, currentValue) {
        this.elems = new Set();
        for(var i = 0; i < this.results.length; i++) {
            this.elems.add(this.results[i].subject);
        }
        this.elems = Array.from(this.elems);
        this.currentElem = currentValue;
        this.left = true;
        this.updateIdx = idx;
        $('#updateElement').modal('show');
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

    },
    removeLink(idx) {
        this.results.splice(idx,1);
        this.fn--;
    },
    save() {
        
        jQuery.ajax({
            url: "/manage/save/<%= request.getParameter("id") %>",
            data: JSON.stringify({"identifier":"<%= request.getParameter("id") %>", "precision": this.precnum(), "recall": this.recnum(), "fmeasure": this.fmnum(), "results": this.results}),
            method: "POST",
            processData: false,
            success: function() {
            },
            error: function(er){ document.write(er.responseText); }
        });
    },
    rerun() {
        alert("TODO");
    },
    retrain() {
        alert("TODO");
    }
  }
});
</script>
    </body>
</html>

