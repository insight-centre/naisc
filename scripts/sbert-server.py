## The server for offering SBERT similarity to Naisc
from sentence_transformers import SentenceTransformer
from flask import Flask, json, make_response
from flask_restful import request
from numpy import dot
from numpy.linalg import norm
from functools import lru_cache

model = SentenceTransformer('bert-base-nli-mean-tokens')

api = Flask(__name__)

@api.route('/naisc/default/text_features', methods=['POST'])
def sbert_sim():
    print(request.json)
    if request.json and "string1" in request.json and "string2" in request.json:
        e0 = encode(request.json["string1"])
        e1 = encode(request.json["string2"])
        sim = dot(e0,e1) / norm(e0) / norm(e1)
        return "[{\"name\":\"sbert-cosine\",\"value\":%.8f}]" % sim
    else:
        return make_response("Arguments not set", 400)

@lru_cache(maxsize=10000)
def encode(string):
    return model.encode([string])[0]

if __name__ == "__main__":
    api.run(debug=True)
    
