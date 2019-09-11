import rdflib
import sys

dataset = sys.argv[1]

aligns = rdflib.Graph()
aligns.load("datasets/" + dataset + "/align.nt", format="nt")

used_lefts = {}
used_rights = {}

for s, _, o in aligns:
    used_lefts[s] = s
    used_rights[o] = o

left = rdflib.Graph()
left.load("datasets/" + dataset + "/left.rdf")

with open("datasets/" + dataset + "/left.ttl", "wb") as out:
    g2 = rdflib.Graph()
    for s, p, o in left:
        if s in used_lefts:
            g2.add((s,p,o))
    out.write(g2.serialize(format="turtle"))

right = rdflib.Graph()
right.load("datasets/" + dataset + "/right.rdf")

with open("datasets/" + dataset + "/right.ttl", "wb") as out:
    g2 = rdflib.Graph()
    for s, p, o in right:
        if s in used_rights:
            g2.add((s,p,o))
    out.write(g2.serialize(format="turtle"))
         
