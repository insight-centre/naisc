import rdflib
import sys
import re

dataset = sys.argv[1]

aligns = rdflib.Graph()
aligns.load("datasets/" + dataset + "/align.nt", format="nt")

used_lefts = {}
used_rights = {}

for s, _, o in aligns:
    used_lefts[str(s)] = s
    used_rights[str(o)] = o

desc_line = re.compile(".*<rdf:Description rdf:about=\"(.*)\">.*")

def do_filter(name, used):
    with open("datasets/" + dataset + "/" + name + ".rdf") as inf:
        with open("datasets/" + dataset + "/" + name + "2.rdf", "w") as out:
            line = inf.readline()
            while line != ">\n":
                out.write(line)
                line = inf.readline()
            out.write(line)

            in_entity = False
            while line:
                m = desc_line.match(line)
                if m:
                    if m.group(1) in used:
                        out.write(line)
                        in_entity = True
                    else:
                        in_entity = False
                elif line == "  </rdf:Description>":
                    if in_entity:
                        out.write(line)
                    in_entity = False
                elif in_entity:
                    out.write(line)
                line = inf.readline()
            out.write("</rdf:RDF>")

do_filter("left",used_lefts)
do_filter("right",used_rights)

#left = rdflib.Graph()
#left.load("datasets/" + dataset + "/left.rdf")
#
#with open("datasets/" + dataset + "/left.ttl", "wb") as out:
#    g2 = rdflib.Graph()
#    for s, p, o in left:
#        if s in used_lefts:
#            g2.add((s,p,o))
#    out.write(g2.serialize(format="turtle"))
#
#right = rdflib.Graph()
#right.load("datasets/" + dataset + "/right.rdf")
#
#with open("datasets/" + dataset + "/right.ttl", "wb") as out:
#    g2 = rdflib.Graph()
#    for s, p, o in right:
#        if s in used_rights:
#            g2.add((s,p,o))
#    out.write(g2.serialize(format="turtle"))
#         
