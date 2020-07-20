# Naisc Configuration Guidelines

Naisc has a number of ways that it can be configured through the use of Json configuration files,
this document describes all the configuration parameters that are available for Naisc. This document 
is auto-generated based on the annotations in the codebase.

The configuration file consists of the following sections:

* `blocking`
* `lenses`
* `textFeatures`
* `graphFeatures`
* `scorers`
* `matchers`
* `rescaler`
* Other global properties

Each of these is described in more detail in the following section. Every component is specified by an
object with the property `name` to indicate the component to be used.

## Blocking Strategy Configurations

Blocking strategies occur in the `blocking` section of the configuration. There is only a single blocking 
strategy so value of `blocking` is an object with a `name` property.


### Automatic

The smart, automatic matching strategy that builds on the analysis of the datasets to find potential matches. This setting should be used most of the time

**Name:** `blocking.Automatic`

#### Configuration Parameters

* `maxMatches`: The maximum number of candidates to generate per entity *(int)*
* `ngrams`: The character n-gram to use in matching (Default value: 3) *(int)*

### All

This blocking strategy matches all possible URIs between the two datasets. It has no configuration parameters.

**Name:** `blocking.All`
No parameters

### IDMatch

Match according to the identifier. This is used in the case of a dataset where the linking is already known (by the URI) and the goal is to find the semantic similarity. When using this setting pre-linking should be disabled

**Name:** `blocking.IDMatch`

#### Configuration Parameters

* `method`: The method to match (Default value: "endOfPath") *(One of exact|fragment|endOfPath|namespace)*
* `leftNamespace`: The namespace for matching elements in the left dataset (Default value: "") *(String)*
* `rightNamespace`: The namespace for matching elements in the right dataset (Default value: "") *(String)*

### Label Match

This setting assumes that there is a matching label that indicates candidates. This can be used for example for dictionary sense linking where the goal is to match senses with the same entry, although note the same behaviour is implemented by the `OntoLex` linker

**Name:** `blocking.LabelMatch`

#### Configuration Parameters

* `property`: The label property to match on (Default value: "http://www.w3.org/2000/01/rdf-schema#label") *(String)*
* `rightProperty`: The label property in the right datasets (if different from left) (Default value: "") *(String)*
* `language`: The language to match on, as an ISO 639 code (Default value: "en") *(String)*
* `mode`: The mode to match; strict for exact matching, or lenient for partial (Default value: "strict") *(One of strict|lenient)*
* `lowercase`: Whether to lowercase labels before matching (Default value: true) *(boolean)*

### Approximate String Matching

String matching generates a blocking that consists of the most similar entities between the two datasets based on a string label. It can be implemented with either Levenshtein or N-Gram similarity

**Name:** `blocking.ApproximateStringMatching`

#### Configuration Parameters

* `maxMatches`: The maximum number of matches to return per entity *(int)*
* `property`: The property to use to find a text label (Default value: http://www.w3.org/2000/01/rdf-schema#label) *(String)*
* `rightProperty`: The property to use in the right dataset. If this is null or omitted then the `property` is used for both the left and right dataset *(String)*
* `queueMax`: The maximum size of the queue (sets the default queue size, 0 for no limit, only for Levenshtein) *(int)*
* `metric`: The string similarity metric to use (Default value: ngrams) *(One of levenshtein|ngrams)*
* `ngrams`: The maximum size of character n-gram to use in matching (Default value: 3) *(int)*
* `lowercase`: Use case-insensitive matching (Default value: true) *(boolean)*
* `type`: Type of the element. If set all matched elements are of rdf:type with this URI *(String)*

### Predefined

Used when the blocking is already known. This blocker simply loads a blocking from a file and returns it.

**Name:** `blocking.Predefined`

#### Configuration Parameters

* `links`: The path to the file containing the links to produce *(String)*

### Onto Lex

This is used to create a monolingual word sense alignment between two dictionaries in the OntoLex-Lemon format

**Name:** `blocking.OntoLex`
No parameters

### Command

**Name:** `blocking.Command`

#### Configuration Parameters

* `command`: The command to run it should have to slots $SPARQL_LEFT and $SPARQL_RIGHT for the URL of the left and right SPARQL endpoint *(String)*

### Path

This blocking strategy uses the graph distance based on a number of pre-linked elements. This means that this blocker first looks for a set of elements where there is a value shared by exactly two elements in the left and right dataset, and then returns as candidates all elements that are within n hops in the graph from one of these pre-links

**Name:** `blocking.Path`

#### Configuration Parameters

* `maxMatches`: The maximum number of nodes to explore in the path method *(int)*
* `preblockLeftProperty`: The property to use in the left side of the pre-blocking *(String)*
* `preblockRightProperty`: The property to use in the right side of the pre-blocking (or empty for same as left) *(String)*


## Lens Configurations

Lens configuration is given in the `lenses` section of the configuration. There may be multiple lenses and as 
such the `lenses` parameter takes an array of objects, where each object has a `name`.


### Label

Extract a string from a pair of entities by a single property

**Name:** `lens.Label`

#### Configuration Parameters

* `property`: The property to extract (Default value: ["http://www.w3.org/2000/01/rdf-schema#label"]) *(String)*
* `rightProperty`: The property to extract (Default value: ["http://www.w3.org/2000/01/rdf-schema#label"]) *(String)*
* `language`: The language to extract (Default value: null) *(String)*
* `id`: The unique identifier of this lens *(String)*

### URI

Extract a label from the URI itself by de-camel-casing the final part of the URI string

**Name:** `lens.URI`

#### Configuration Parameters

* `location`: The location of the label in the URL *(One of fragment|endOfPath|infer)*
* `form`: The form (camelCased, under_scored) of the label *(One of camelCased|underscored|urlEncoded|smart)*
* `separator`: The character that separates words in the label *(String)*

### SPARQL

A lens that is implemented by a SPARQL query. The query should return exactly two string literals and should contain the special variables $entity1 and $entity2. For example
```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT ?label1 ?label2 WHERE { 
   $entity1 rdfs:label ?label1 .
   $entity2 rdfs:label ?label2 .
}
```

**Name:** `lens.SPARQL`

#### Configuration Parameters

* `query`: The SPARQL query *(String)*
* `baseURI`: A base URI for the query (optional) *(String)*

### Onto Lex

Analyse a dataset according to the OntoLex model and extract labels accordingly

**Name:** `lens.OntoLex`

#### Configuration Parameters

* `dialect`: The dialect (namespace) to use *(One of ONTOLEX|LEMON|MONNET_LEMON)*
* `onlyCanonical`: Only use canonical forms or use all forms *(boolean)*
* `language`: The language to extract, null for first language available *(String)*

### Command

**Name:** `lens.Command`

#### Configuration Parameters

* `command`: The command to run, the sparql endpoint for the data will be provided as $SPARQL *(String)*
* `id`: The identifier of this feature extractor *(String)*


## Text Feature Configurations

Text features are given in the `textFeatures` section of the configuration. There may be multiple text features
so  the `textFeatures` parameter takes an array of objects, where each object has a `name`. In addition, you may
provide a `tags` parameter to any text feature, which selects the lenses it may use.


### Bag Of Words Sim

Similarity based on bag of words

**Name:** `feature.BagOfWordsSim`

#### Configuration Parameters

* `method`: The similarity method to use *(One of jaccard|jaccardExponential)*
* `weighting`: The weighting value. Near-zero values will penalize low agreement morewhile high values will be nearly binary *(double)*
* `lowerCase`: Whether to lowercase the text before processing *(boolean)*

### Basic String

Basic language-independent string-based similarity

**Name:** `feature.BasicString`

#### Configuration Parameters

* `labelChar`: Also extract character-level features *(boolean)*
* `wordWeights`: Weight the words according to this file *(String)*
* `ngramWeights`: Weight the character n-grams according to this file *(String)*
* `features`: The features to extract (Default value: null) *(List of One of lcs|lc_prefix|lc_suffix|ngram_1|ngram_2|ngram_3|ngram_4|ngram_5|jaccard|dice|containment|senLenRatio|aveWordLenRatio|negation|number|jaroWinkler|levenshtein|mongeElkanJaroWinkler|mongeElkanLevenshtein)*
* `lowerCase`: Convert all strings to lower case before processing (Default value: true) *(boolean)*

### Dictionary

Check for synonyms in a dictionary

**Name:** `feature.Dictionary`

#### Configuration Parameters

* `dict`: The dictionary to use (tab-separated synonyms, one per line) *(String)*

### Key Words

Keywords feature measures the Jaccard/Dice overlap of a set of key terms.

**Name:** `feature.KeyWords`

#### Configuration Parameters

* `keywordsFile`: The file containing the key words *(String)*

### Word Embeddings

Similarity based on word embeddings. This method creates a grid of word similarity 

**Name:** `feature.WordEmbeddings`

#### Configuration Parameters

* `embeddingPath`: The path to the embeddings file *(String)*
* `features`: The features to use; values include "fp", "bp", "ham", "max", "max2", "max.5",
        "max.1", "collp2", "collp10", "Hg" *(List of String)*
* `saliencyFile`: The path to the saliency values *(String)*
* `stopwords`: The stopwords file (if used) *(String)*

### Word Net

Similarity based on the overlap of synonymous and closely related words according to WordNet

**Name:** `feature.WordNet`

#### Configuration Parameters

* `wordnetXmlFile`: The path to the WordNet file in GWA XML format *(String)*
* `methods`: The methods to use *(List of One of SHORTEST_PATH|WU_PALMER|LEAKCOCK_CHODOROW|LI)*

### Command

**Name:** `feature.Command`

#### Configuration Parameters

* `command`: The command to run *(String)*
* `id`: The identifier *(String)*

### Machine Translation

String similarity methods based on those widely-used for the evaluation of machine translation

**Name:** `feature.MachineTranslation`

#### Configuration Parameters

* `methods`: The methods to use (Default value: ["BLEU", "BLEU-2", "chrF", "METEOR", "NIST", "TER"]) *(List of One of BLEU|BLEU2|chrF|METEOR|NIST|TER)*
* `bleuN`: The n-gram to use for BLEU (Default value: 4) *(int)*
* `bleuN2`: The n-gram to use for the second BLEU (Default value: 2) *(int)*
* `chrFN`: The n-gram size for chrF (Default value: 6) *(int)*
* `chrFbeta`: The beat paramater for chrF (Default value: 3) *(int)*
* `nistN`: The n-gram size for NIST (Default value: 4) *(int)*


## Graph Feature Configurations

Graph features are given in the `graphFeatures` section of the configuration. There may be multiple graph features
so the `graphFeatures` parameter takes an array of objects, where each object has a `name`.


### Property Overlap

Measures the overlap of two entities by properties that they both have. This is useful if there are properties such as part-of-speech or type that can guide the linking

**Name:** `graph.PropertyOverlap`

#### Configuration Parameters

* `properties`: The set of properties to use for overlap or empty for no properties *(Set of String)*

### Command

**Name:** `graph.Command`

#### Configuration Parameters

* `command`: The command to run, the sparql endpoint for the data will be provided as $SPARQL *(String)*
* `id`: The identifier of this feature extractor *(String)*

### Automatic

The smart, automatic matching strategy that builds on the analysis of the datasets to find potential matches. This setting should be used most of the time

**Name:** `blocking.Automatic`

#### Configuration Parameters

* `maxMatches`: The maximum number of candidates to generate per entity *(int)*
* `ngrams`: The character n-gram to use in matching (Default value: 3) *(int)*

### PPR

The Personalised PageRank metric estimates how close two elements in the two datasets. This method relies on pre-links being constructed between the two datasets. The implementation is based on Lofgren, Peter A., et al. "FAST-PPR: scaling personalized pagerank estimation for large graphs." and details of the parameters are in the paper

**Name:** `graph.PPR`
No parameters


## Scorer Configurations

Scorers are given in the `scorers` section of the configuration. There may be multiple scorers (associated with
predicting different properties) so this parameter takes an array of objects, where each object has a `name`.


### Average

The scorer simply averages the weight of the scores generated

**Name:** `scorer.Average`

#### Configuration Parameters

* `weights`: The weights to be applied to the features *(double[])*
* `property`: The property to predict *(String)*
* `softmax`: Apply a soft clipping of average using the sigmoid function *(boolean)*

### Lib SVM

This scorer learns and applies an optimal scoring given the features. This is a supervised method and must be trained in advance. It is not robust to changes in the features generated so cannot easily be applied to other datasets

**Name:** `scorer.LibSVM`

#### Configuration Parameters

* `property`: The property to output *(String)*
* `perFeature`: Print analysis of features *(boolean)*

### Command

**Name:** `scorer.Command`

#### Configuration Parameters

* `command`: The command to run. Use $MODEL_PATH to indicate the path to the model. *(String)*
* `trainCommand`: The command to run the trainer. Use $MODEL_PATH to indicate the path to the model. *(String)*
* `property`: The property to output *(String)*

### RAd LR

Robust Adaptive Linear Regression. This scorer is based on linear regression but can produce reasonable results for unseen features (assuming some positive correlation). This works better as a supervised model (although not as well as SVM) but is more robust and effective as an unsupervised method as well

**Name:** `scorer.RAdLR`

#### Configuration Parameters

* `errorFunction`: The error function to use in training *(One of KullbackLeibler|SoftkullbackLeibler|FMeasure)*


## Matcher Configuration

The matcher is given in the `matcher` section of the configuration. It should be a single object with a `name`.


### Threshold

Simple matcher that outputs all links over a certain score threshold

**Name:** `matcher.Threshold`

#### Configuration Parameters

* `threshold`: The threshold to accept *(double)*

### Unique Assignment

A special matcher that implements the Hungarian algorithm (a.k.a. MunkRes) to find a matching that gives the highest score given that no element is linked to more than one element in the other dataset

**Name:** `matcher.UniqueAssignment`

#### Configuration Parameters

* `threshold`: The minimum threshold to accept *(double)*
* `baseProbability`: The probability assigned to non-scored examples *(double)*

### Greedy

The greedy matcher finds a solution given an arbitrary constraint quickly by always taking the highest scoring link. It may produce poorer results than other methods

**Name:** `matcher.Greedy`

#### Configuration Parameters

* `constraint`: The constraint that the searcher will optimize *(Constraint - see 'Constriants' section)*
* `threshold`: The threshold (minimum value to accept) *(double)*

### Beam Search

Beam search finds a matching according to a generic constraint by keeping a list of top solutions found during the search

**Name:** `matcher.BeamSearch`

#### Configuration Parameters

* `constraint`: The constraint that the searcher will optimize *(Constraint - see 'Constriants' section)*
* `threshold`: The threshold (minimum value to accept) *(double)*
* `beamSize`: The size of beam. Trades the speed and memory usage of the algorithm off with the quality of the solution *(int)*
* `maxIterations`: The maxiumum number of iterations to perform (zero for no limit) *(int)*

### Command

**Name:** `matcher.Command`

#### Configuration Parameters

* `command`: The command to run *(String)*

### Monte Carlo Tree Search

Find a matching that satisifies an arbitrary constraint by means of the Monte-Carlo Tree Search algorithm

**Name:** `matcher.MonteCarloTreeSearch`

#### Configuration Parameters

* `ce`: The exploration parameter (expert) *(double)*
* `maxIterations`: The maximum number of iterations to perform *(int)*
* `constraint`: The constraint that the searcher will optimize *(Constraint - see 'Constriants' section)*


## Constraint Configuration

Constraints are elements based to some matchers that restrict the kind of linking Naisc can produce. It should be a single object with a `name`.


### Threshold Constraint

A simple constraint that says that the score must be over a threshold

**Name:** `constraint.ThresholdConstraint`

#### Configuration Parameters

* `threshold`: The minimum threshold to accept *(double)*

### Bijective

The bijective constraint requires that no more than one link exists for each element on the source and/or target dataset

**Name:** `constraint.Bijective`

#### Configuration Parameters

* `surjection`: The type of constraint: *bijective* means at most one link on the source and target side, *surjective* means at most one link on the source side, and *inverseSurjective* means at most one link on the target side (Default value: bijective) *(One of surjective|inverseSurjective|bijective)*


## Rescaler Configuration (experimental)

Rescalers are still experimental, currently you can only configure to use one of the following methods:

* `NoScaling`: Do not rescale the results of the scorer
* `MinMax`: Rescale the results of the scorer so that the highest prediction is 1 and the lowest is 0
* `Percentile`: Rescale so that the values correspond to the percentile of values that have this value. e.g., 0.5 means that score is exactly the mode of the dataset

## Other parameters

The following further parameters are supported by Naisc:
* `nThreads`: The maximum number of threads to use when aligning *(int > 0)*
* `includeFeatures`: The calculated features will be included in the output alignments (can make the alignment files very large!) *(boolean)*
* `ignorePreexisting`: If there are any links between the datasets already they will be discarded and Naisc will only infer new links *(boolean)*
* `noPrematching`: Do not attempt to find unambiguous links and use the full pipeline for every link inference *(boolean)*

