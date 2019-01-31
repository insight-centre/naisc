#####
## Convert a dataset from the previous Naisc format to the new one
import sys
import json
import os

old_data = json.load(open(sys.argv[1]))

if "/" in sys.argv[1]:
    dataset_name = sys.argv[1][(sys.argv[1].rindex("/") + 1):-5]
else:
    dataset_name = sys.argv[1][:-5]

if not os.path.exists("datasets/" + dataset_name):
    os.mkdir("datasets/" + dataset_name)

left = old_data["alignments"][0]["left"]
right = old_data["alignments"][0]["right"]

def clean_id(ident):
    return ident.replace(" ", "_")

def map_type(t):
    if t == "Class":
        return "http://www.w3.org/2002/07/owl#Class"
    if t == "Property":
        return "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"
    if t == "ObjectProperty":
        return "http://www.w3.org/2002/07/owl#ObjectProperty"
    if t == "DataProperty":
        return "http://www.w3.org/2002/07/owl#DatatypePropety"
    if t == "AnnotationProperty":
        return "http://www.w3.org/2002/07/owl#AnnotationPropety"
    if t == "Individual":
        return "http://www.w3.org/2002/07/owl#NamedIndividual"
    if t == "Datatype":
        return "http://www.w3.org/2000/01/rdf-schema#Datatype"
    if t == "Noun":
        return "http://www.lexinfo.net/ontology/2.0/lexinfo#Noun"
    if t == "Verb":
        return "http://www.lexinfo.net/ontology/2.0/lexinfo#Noun"
    if t == "Adjective":
        return "http://www.lexinfo.net/ontology/2.0/lexinfo#Noun"
    if t == "Adverb":
        return "http://www.lexinfo.net/ontology/2.0/lexinfo#Noun"
    if t == "Other":
        return "http://www.w3.org/2002/07/owl#Thing"

def map_rel(r):
    if r == "equivalent":
        return "http://www.w3.org/2004/02/skos/core#exactMatch"
    if r == "broader":
        return "http://www.w3.org/2004/02/skos/core#broader"
    if r == "narrower":
        return "http://www.w3.org/2004/02/skos/core#narrower"
    if r == "incompatible":
        return "http://www.example.com/incompatible"
    if r == "related":
        return "http://www.w3.org/2004/02/skos/core#related"
    if r == "closeMatch":
        return "http://www.w3.org/2004/02/skos/core#closeMatch"
    if r == "exactMatch":
        return "http://www.w3.org/2004/02/skos/core#exactMatch"
    if r == "antonym":
        return "http://wordnet-rdf.princeton.edu/ontology#antonym"
    if r == "meronym":
        return "http://wordnet-rdf.princeton.edu/ontology#meronym"
    if r == "holonym":
        return "http://wordnet-rdf.princeton.edu/ontology#holonym"
    if r == "topic":
        return "http://wordnet-rdf.princeton.edu/ontology#topic"
    if r == "domain":
        return "http://wordnet-rdf.princeton.edu/ontology#domain"
    if r == "range":
        return "http://wordnet-rdf.princeton.edu/ontology#range"
    if r == "inverse":
        return "http://wordnet-rdf.princeton.edu/ontology#inverse"
    if r == "instance":
        return "http://wordnet-rdf.princeton.edu/ontology#instance"
    if r == "isInstanceOf":
        return "http://wordnet-rdf.princeton.edu/ontology#isInstanceOf"
    if r == "other":
        return "http://wordnet-rdf.princeton.edu/ontology#other"


with open("datasets/" + dataset_name + "/left.nt", "w") as out:
    for entity in old_data["left"][left]["entities"]:
        if "label" in entity:
            for lang, labels in entity["label"].items():
                for label in labels:
                    out.write("<file:%s/%s#%s> <http://www.w3.org/2000/01/rdf-schema#label> \"%s\"@%s .\n" %
                            (dataset_name, left, clean_id(entity["@id"]), label, lang))
        if "description" in entity:
            for lang, descriptions in entity["description"].items():
                for desc in descriptions:
                    out.write("<file:%s/%s#%s> <http://www.w3.org/2004/02/skos/core#description> \"%s\"@%s .\n" %
                            (dataset_name, left, clean_id(entity["@id"]), label, lang))
        if "type" in entity:
            out.write("<file:%s/%s#%s> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <%s> .\n" %
                    (dataset_name, left, entity["@id"], map_type(entity["type"])))
        if "relation" in entity:
            for rel, targets in entity["relation"].items():
                for target in targets:
                    out.write("<file:%s/%s#%s> <%s> <file:%s/%s#%s> .\n" %
                        (dataset_name, left, clean_id(entity["@id"]), map_rel(rel), dataset_name, left, clean_id(target)))


with open("datasets/" + dataset_name + "/right.nt", "w") as out:
    for entity in old_data["right"][right]["entities"]:
        if "label" in entity:
            for lang, labels in entity["label"].items():
                for label in labels:
                    out.write("<file:%s/%s#%s> <http://www.w3.org/2000/01/rdf-schema#label> \"%s\"@%s .\n" %
                            (dataset_name, right, clean_id(entity["@id"]), label, lang))
        if "description" in entity:
            for lang, descriptions in entity["description"].items():
                for desc in descriptions:
                    out.write("<file:%s/%s#%s> <http://www.w3.org/2004/02/skos/core#description> \"%s\"@%s .\n" %
                            (dataset_name, right, clean_id(entity["@id"]), label, lang))
        if "type" in entity:
            out.write("<file:%s/%s#%s> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <%s> .\n" %
                    (dataset_name, right, entity["@id"], map_type(entity["type"])))
        if "relation" in entity:
            for rel, targets in entity["relation"].items():
                for target in targets:
                    out.write("<file:%s/%s#%s> <%s> <file:%s/%s#%s> .\n" %
                        (dataset_name, right, clean_id(entity["@id"]), map_rel(rel), dataset_name, left, clean_id(target)))

with open("datasets/" + dataset_name + "/align.nt", "w") as out:
    for align in old_data["alignments"][0]["alignments"]:
        if align["relation"] != "equivalent":
            print("Unknown relation!")
        out.write("<file:%s/%s#%s> <http://www.w3.org/2004/02/skos/core#exactMatch> <file:%s/%s#%s> . # %s\n" %
                (dataset_name, left, clean_id(align["entity1"]), 
                 dataset_name, right, clean_id(align["entity2"]), align["score"]))

