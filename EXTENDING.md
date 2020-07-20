# Extending Naisc

Naisc provides a strong experimental framework and it is easy to extend Naisc 
to add new features and improved NLP technologies. In this document we will show
a couple of examples of extending the Naisc system, in particular by means of a
new Java service and by an external REST service.

## Extending Naisc in Java

Naisc is easily extendable by means of creating a new Java service. This is done
by implementing the interface and the corresponding `Factory` interface. For
example to add a new scorer you would implement


**ScorerFactory.java**
```java
public interface ScorerFactory {
    Scorer makeScorer(Map<String, Object> params, File modelPath) throws IOException;

    Option<ScorerTrainer> makeTrainer(Map<String, Object> params, String property, File modelPath);
}
```

**Scorer.java**
```java
public interface Scorer extends Closeable { 
    List<ScoreResult> similarity(FeatureSet features, NaiscListener log) throws ModelNotTrainedException;
}
```

A simple implementation of the scorer would then implement both of these interfaces

**MyScorer.java**
```java
public class MyScorer implements ScorerFactory {
    @Override public Scorer makeScorer(Map<String, Object> params, File modelPath) throws IOException {
        return new MyScorerImpl();
    }

    public static class MyScorerImpl implements Scorer {
        @Override public List<ScoreResult> similarity(FeatureSet features, NaiscListener log) throws ModelNotTrainedException {
            return Arrays.asList(new ScoreResult(1.0, SKOS_EXACT_MATCH));
        }

        @Override public close() throws IOException {
        }
    }
}
```

We can then add this to a configuration in order to test for example we could
extend the `auto` configuration

**configs/myconfig.json**
```json
{
    "blocking": {
        "name": "blocking.Automatic"
    },
    "lenses": [],
    "textFeatures": [{
        "name": "feature.BasicString",
        "wordWeights": "models/idf",
        "ngramWeights": "models/ngidf",
        "labelChar": true
    },{
        "name": "feature.WordEmbeddings",
        "embeddingPath": "models/glove.6B.100d.txt"
    },{
        "name": "com.package.MyScorer",
        "param": "value"
    }],
    "graphFeatures": [{
        "name": "graph.Automatic"
    }],
    "scorers": [{
        "name": "scorer.RAdLR",
        "modelFile": "models/auto.radlr"
    }],
    "matcher": {
        "name": "matcher.Greedy",
        "constraint": {
            "name": "constraint.Bijective"
        }
    },
    "description": "The setting using my new scorer"
}
```

The changes are the addition of the new scorer, referenced by its full class name
including all the packages (for internal Naisc services it is assumed the initial
packages `org.insightcentre.uld.naisc` is optional). Any parameters that can be
passed to the factory function can also be specified. Then the description of the
configuration should also be updated.


## Extending Naisc by a REST service

If you wish to extend Naisc by using an implementation written in another language
this can be done by setting up a REST service. The specifications for each of 
these is given at

https://app.swaggerhub.com/apis/jmccrae/Naisc/1.0#/

In this example we will implement a text feature. To implement this feature, we
need to create a REST interface that takes a `POST` request to 
`/naisc/{config}/text_features`. For simplicity we will ignore the `config`
parameter and assume it is always `default`. 

This endpoint will be passed a document that looks something like:

```json
{
    "string1": "This is the first string",
    "lang1": "en",
    "string2": "This is the second string",
    "lang2": "en",
    "tag": "tag"
}
```

Where the two strings are the elements to be compared and the two language fields
give the languages of the string as extracted from the dataset. The `tag` field
gives some metadata about how the text was extracted and is ignored for this 
example.

The expected result is also a JSON document that looks something like this:

```json
[{
    "name": "My Feature",
    "value": 0.6
}]
```

This returns an array of features that we have extracted, each with a numeric 
value. 

For this example we will use the [Sentence BERT](https://github.com/UKPLab/sentence-transformers)
model developed by the UKP lab at the University of Darmstadt. We will return
a single feature consisting of the cosine similarity of the embeddings according 
to this model. In this example, we use [Flask RESTful](https://flask-restful.readthedocs.io/en/latest/)
and [NumPy](https://numpy.org/).

**scripts/sbert-server.py**
```python
from sentence_transformers import SentenceTransformer
from flask import Flask, json, make_response
from flask_restful import request
from numpy import dot
from numpy.linalg import norm

model = SentenceTransformer('bert-base-nli-mean-tokens')

api = Flask(__name__)

@api.route('/naisc/default/text_features', methods=['POST'])
def sbert_sim():
    if request.json and "string1" in request.json and "string2" in request.json:
        sentences = [request.json["string1"], request.json["string2"]]
        se = model.encode(sentences)
        sim = dot(se[0],se[1]) / norm(se[0]) / norm(se[1])
        return "[{\"name\":\"sbert-cosine\",\"value\":%.8f}]" % sim
    else:
        return make_response("Arguments not set", 400)

if __name__ == "__main__":
    api.run(debug=True)
```

We can start this service simply by executing it with Python

```
python3 scripts/sbert-server.py
```

Then we can simply check that this works with a cURL command as follows:

```
curl -X POST -d "{\"string1\":\"foo\",\"string2\":\"bar\"}" -H "Content-type: application/json" http://localhost:5000/naisc/default/text_features
```

In order to make this work with Naisc, we need to create a new configuration as
in the previous example. This involves adding a feature using the `ExternalTextFeature`
implementation. For brevity we omit the unchanged lines.

**configs/bert.json**
```json
{
    ...
    "textFeatures": [{
    ...
        "name": "feature.ExternalTextFeature",
        "endpoint": "http://localhost:5000/"
    }],
    ...
    "description": "The default setting with Sentence BERT cosine simiarity"
}
```

This example is implemented in the project, with some minor further improvements.
