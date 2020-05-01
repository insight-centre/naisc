## Converts the MWSA ELEXIS shared data task to be used by Naisc
import sys
import os
from urllib.parse import quote

BASE_URL = "http://elex.is/mwsa/"

term2id = {}

def process_file(f):
    with open(f) as inp:
        lterms = []
        rterms = []
        links = []
        for line in inp.readlines():
            e = line.split("\t")
            assert(len(e) == 5)
            lterm = (e[0], e[1], e[2])
            rterm = (e[0], e[1], e[3])
            if lterm not in term2id:
                term2id[lterm] = len(term2id)
                lterms.append(lterm)
            if rterm not in term2id:
                term2id[rterm] = len(term2id)
                rterms.append(rterm)
            if e[4].strip() == "":
                print("%s %s %s" % (lterm, rterm, e[4]))
            links.append((term2id[lterm], term2id[rterm], e[4]))
    return lterms, rterms, links

def to_skos(r):
    if r == "exact":
        return "exactMatch"
    elif r == "narrower":
        return "narrowMatch"
    elif r == "broader":
        return "broadMatch"
    elif r == "related":
        return "relatedMatch"
    else:
        print("bad property "+ r)
        sys.exit(-1)

def to_lang(l):
    if "_" in l:
        return l.split("_")[0]
    else:
        return l

def escape_literal(l):
    return l.replace("\"","\\\"")

def write_dataset(name, lterms):
    with open("datasets/mwsa_%s/%s.nt" % (sys.argv[3], name), "w") as left:
        for (lemma, pos, defn) in lterms:
            entry_id = "<%s#%s_%s>" % (BASE_URL, quote(lemma), quote(pos))
            sense_id = "<%s#sense%d>" % (BASE_URL, term2id[(lemma, pos, defn)]) 
            left.write("%s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/lemon/ontolex#LexicalEntry> .\n" % entry_id)
            left.write("%s <http://www.w3.org/ns/lemon/ontolex#sense> %s .\n" % (entry_id, sense_id))
            left.write("%s <http://www.w3.org/2000/01/rdf-schema#label> \"%s\"@%s .\n" % (entry_id, escape_literal(lemma), to_lang(sys.argv[3])))
            left.write("%s <http://www.w3.org/2004/02/skos/core#definition> \"%s\"@%s .\n" % (sense_id, escape_literal(defn), to_lang(sys.argv[3])))
 

def write_align(name, links):
    with open("datasets/mwsa_%s/%s.nt" % (sys.argv[3], name), "w") as align:
        for (id1, id2, link_type) in links:
            if link_type.strip() != "none":
                align.write("<%s#sense%d> <http://www.w3.org/2004/02/skos/core#%s> <%s#sense%d> .\n" % (BASE_URL, id1, to_skos(link_type.strip()), BASE_URL, id2))

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python scripts/mwsa-to-naisc.py input_data/train/english.tsv reference/english.tsv en")
        sys.exit(-1)

    lterms, rterms, links_train = process_file(sys.argv[1])
    lterms2, rterms2, links_test = process_file(sys.argv[2])
    lterms += lterms2
    rterms += rterms2

    if not os.path.isdir("datasets/mwsa_%s" % sys.argv[3]):
        os.mkdir("datasets/mwsa_%s" % sys.argv[3])
    write_dataset("left", lterms)
    write_dataset("right", rterms)
    write_align("align-train", links_train)
    write_align("align", links_test)
