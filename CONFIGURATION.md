# Naisc Configuration Guidelines

Naisc has a number of ways that it can be configured through the use of Json configuration files,
this document describes all the configuration parameters that are available for Naisc. This document 
is auto-generated based on the annotations in the codebase.

## Blocking Strategy Configurations

### Automatic

**Name:** blocking.Automatic

#### Configuration Parameters

* **maxMatches**:*No Description* *<int>*
* **ngrams**:*No Description* *<int>*

### All

**Name:** blocking.All

#### Configuration Parameters


### IDMatch

**Name:** blocking.IDMatch

#### Configuration Parameters

* **method**:The method to match (Default value: "endOfPath") *One of exact|fragment|endOfPath|namespace*
* **leftNamespace**:The namespace for matching elements in the left dataset (Default value: "") *<String>*
* **rightNamespace**:The namespace for matching elements in the right dataset (Default value: "") *<String>*

### Label Match

**Name:** blocking.LabelMatch

#### Configuration Parameters

* **property**:The label property to match on (Default value: "http://www.w3.org/2000/01/rdf-schema#label") *<String>*
* **rightProperty**:The label property in the right datasets (if different from left) (Default value: "") *<String>*
* **language**:The language to match on, as an ISO 639 code (Default value: "en") *<String>*
* **mode**:The mode to match; strict for exact matching, or lenient for partial (Default value: "strict") *One of strict|lenient*
* **lowercase**:Whether to lowercase labels before matching (Default value: true) *<boolean>*

### Approximate String Matching

**Name:** blocking.ApproximateStringMatching

#### Configuration Parameters

* **maxMatches**:*No Description* *<int>*
* **property**:*No Description* *<String>*
* **rightProperty**:*No Description* *<String>*
* **queueMax**:*No Description* *<int>*
* **metric**:*No Description* *One of levenshtein|ngrams*
* **ngrams**:*No Description* *<int>*
* **lowercase**:*No Description* *<boolean>*
* **type**:*No Description* *<String>*

### Predefined

**Name:** blocking.Predefined

#### Configuration Parameters

* **links**:The path to the file containing the links to produce *<String>*

### Onto Lex

**Name:** blocking.OntoLex

#### Configuration Parameters


### Command

**Name:** blocking.Command

#### Configuration Parameters

* **command**:The command to run it should have to slots $SPARQL_LEFT and $SPARQL_RIGHT for the URL of the left and right SPARQL endpoint *<String>*

### Path

**Name:** blocking.Path

#### Configuration Parameters

* **maxMatches**:The maximum number of nodes to explore in the path method *<int>*
* **preblockLeftProperty**:The property to use in the left side of the pre-blocking *<String>*
* **preblockRightProperty**:The property to use in the right side of the pre-blocking (or empty for same as left) *<String>*


## Lens Configurations

### Label

**Name:** lens.Label

#### Configuration Parameters

* **property**:The property to extract (Default value: ["http://www.w3.org/2000/01/rdf-schema#label"]) *<String>*
* **rightProperty**:The property to extract (Default value: ["http://www.w3.org/2000/01/rdf-schema#label"]) *<String>*
* **language**:The language to extract (Default value: null) *<String>*
* **id**:The unique identifier of this lens *<String>*

### URI

**Name:** lens.URI

#### Configuration Parameters

* **location**:The location of the label in the URL *One of fragment|endOfPath|infer*
* **form**:The form (camelCased, under_scored) of the label *One of camelCased|underscored|urlEncoded|smart*
* **separator**:The character that separates words in the label *<String>*

### SPARQL

**Name:** lens.SPARQL

#### Configuration Parameters

* **query**:The SPARQL query *<String>*
* **baseURI**:A base URI for the query (optional) *<String>*

### Onto Lex

**Name:** lens.OntoLex

#### Configuration Parameters

* **dialect**:The dialect (namespace) to use *One of ONTOLEX|LEMON|MONNET_LEMON*
* **onlyCanonical**:Only use canonical forms or use all forms *<boolean>*
* **language**:The language to extract, null for first language available *<String>*

### Command

**Name:** lens.Command

#### Configuration Parameters

* **command**:The command to run, the sparql endpoint for the data will be provided as $SPARQL *<String>*
* **id**:The identifier of this feature extractor *<String>*


## Text Feature Configurations

### Bag Of Words Sim

**Name:** feature.BagOfWordsSim

#### Configuration Parameters

* **method**:The similarity method to use *One of jaccard|jaccardExponential*
* **weighting**:The weighting value. Near-zero values will penalize low agreement morewhile high values will be nearly binary *<double>*
* **lowerCase**:Whether to lowercase the text before processing *<boolean>*

### Basic String

**Name:** feature.BasicString

#### Configuration Parameters

* **labelChar**:Also extract character-level features *<boolean>*
* **wordWeights**:Weight the words according to this file *<String>*
* **ngramWeights**:Weight the character n-grams according to this file *<String>*
* **features**:The features to extract (Default value: null) *<List>*
* **lowerCase**:Convert all strings to lower case before processing (Default value: true) *<boolean>*

### Dictionary

**Name:** feature.Dictionary

#### Configuration Parameters

* **dict**:The dictionary to use *<String>*

### Key Words

**Name:** feature.KeyWords

#### Configuration Parameters

* **keywordsFile**:The file containing the key words *<String>*

### Word Embeddings

**Name:** feature.WordEmbeddings

#### Configuration Parameters

* **embeddingPath**:The path to the embeddings file *<String>*
* **features**:The features to use; values include "fp", "bp", "ham", "max", "max2", "max.5",
        "max.1", "collp2", "collp10", "Hg" *<List>*
* **saliencyFile**:The path to the saliency values *<String>*
* **stopwords**:The stopwords file (if used) *<String>*

### Word Net

**Name:** feature.WordNet

#### Configuration Parameters

* **wordnetXmlFile**:The path to the WordNet file in GWA XML format *<String>*
* **methods**:The methods to use *<List>*

### Command

**Name:** feature.Command

#### Configuration Parameters

* **command**:The command to run *<String>*
* **id**:The identifier *<String>*

### Machine Translation

**Name:** feature.MachineTranslation

#### Configuration Parameters

* **methods**:*No Description* *<List>*
* **bleuN**:*No Description* *<int>*
* **bleuN2**:*No Description* *<int>*
* **chrFN**:*No Description* *<int>*
* **chrFbeta**:*No Description* *<int>*
* **nistN**:*No Description* *<int>*


## Graph Feature Configurations

### Property Overlap

**Name:** graph.PropertyOverlap

#### Configuration Parameters

* **properties**:The set of properties to use for overlap or empty for no properties *<Set>*

### Command

**Name:** graph.Command

#### Configuration Parameters

* **command**:The command to run, the sparql endpoint for the data will be provided as $SPARQL *<String>*
* **id**:The identifier of this feature extractor *<String>*

### Automatic

**Name:** blocking.Automatic

#### Configuration Parameters

* **maxMatches**:*No Description* *<int>*
* **ngrams**:*No Description* *<int>*

### PPR

**Name:** graph.PPR

#### Configuration Parameters



## Scorer Configurations

### Average

**Name:** scorer.Average

#### Configuration Parameters

* **weights**:The weights to be applied to the features *<double[]>*
* **property**:The property to predict *<String>*
* **softmax**:Apply a soft clipping of average using the sigmoid function *<boolean>*

### Lib SVM

**Name:** scorer.LibSVM

#### Configuration Parameters

* **property**:The property to output *<String>*
* **perFeature**:Print analysis of features *<boolean>*

### Command

**Name:** scorer.Command

#### Configuration Parameters

* **command**:The command to run. Use $MODEL_PATH to indicate the path to the model. *<String>*
* **trainCommand**:The command to run the trainer. Use $MODEL_PATH to indicate the path to the model. *<String>*
* **property**:The property to output *<String>*

### RAd LR

**Name:** scorer.RAdLR

#### Configuration Parameters

* **errorFunction**:The error function to use in training *One of KullbackLeibler|SoftkullbackLeibler|FMeasure*


## Matcher Configuration

### Threshold

**Name:** matcher.Threshold

#### Configuration Parameters

* **threshold**:The threshold to accept *<double>*

### Unique Assignment

**Name:** matcher.UniqueAssignment

#### Configuration Parameters

* **threshold**:The minimum threshold to accept *<double>*
* **baseProbability**:The probability assigned to non-scored examples *<double>*

### Greedy

**Name:** matcher.Greedy

#### Configuration Parameters

* **constraint**:The constraint that the searcher will optimize *<ConstraintConfiguration>*
* **threshold**:The threshold (minimum value to accept) *<double>*

### Beam Search

**Name:** matcher.BeamSearch

#### Configuration Parameters

* **constraint**:The constraint that the searcher will optimize *<ConstraintConfiguration>*
* **threshold**:The threshold (minimum value to accept) *<double>*
* **beamSize**:The size of beam. Trades the speed and memory usage of the algorithm off with the quality of the solution *<int>*
* **maxIterations**:The maxiumum number of iterations to perform (zero for no limit) *<int>*

### Command

**Name:** matcher.Command

#### Configuration Parameters

* **command**:The command to run *<String>*

### Monte Carlo Tree Search

**Name:** matcher.MonteCarloTreeSearch

#### Configuration Parameters

* **ce**:The exploration paramter (expert) *<double>*
* **maxIterations**:The maxiumum number of iterations to perform *<int>*
* **constraint**:The constraint that the searcher will optimize *<ConstraintConfiguration>*


## Rescaler Configuration (experimental)

