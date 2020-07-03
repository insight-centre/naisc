## The server for offering SBERT similarity to Naisc
from sentence_transformers import SentenceTransformer
from flask import Flask, json, make_response
from flask_restful import request
from numpy import dot
from numpy.linalg import norm

model = SentenceTransformer('bert-base-nli-mean-tokens')

api = Flask(__name__)

@api.route('/naisc/default/text_features', methods=['POST'])
def sbert_sim():
    print(request.json)
    if request.json and "string1" in request.json and "string2" in request.json:
        sentences = [request.json["string1"], request.json["string2"]]
        se = model.encode(sentences)
        sim = dot(se[0],se[1]) / norm(se[0]) / norm(se[1])
        return "[{\"name\":\"sbert-cosine\",value:%.8f}]" % sim
    else:
        return make_response("Arguments not set", 400)


if __name__ == "__main__":
    api.run(debug=True)
    
