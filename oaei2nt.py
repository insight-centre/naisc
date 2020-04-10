##############################################################################
### Converts an OAEI file into an RDF alignment file as required by Naisc
import xml.etree.ElementTree as ET
import sys

def main():
    if len(sys.argv) < 1:
        print("Usage:\n\t python oaei2nt.py oaei.rdf > datasets/name/align.rdf")
        sys.exit(-1)
    data = ET.parse(open(sys.argv[1]))
    for map in data.find("{http://knowledgeweb.semanticweb.org/heterogeneity/alignment}Alignment").findall("{http://knowledgeweb.semanticweb.org/heterogeneity/alignment}map"):
        cell = map.find("{http://knowledgeweb.semanticweb.org/heterogeneity/alignment}Cell")
        e1 = cell.find("{http://knowledgeweb.semanticweb.org/heterogeneity/alignment}entity1").attrib["{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource"]
        e2 = cell.find("{http://knowledgeweb.semanticweb.org/heterogeneity/alignment}entity2").attrib["{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource"]
        probability = cell.find("{http://knowledgeweb.semanticweb.org/heterogeneity/alignment}measure").text
        if cell.find("{http://knowledgeweb.semanticweb.org/heterogeneity/alignment}property").text == "=":
            print("<%s> <http://www.w3.org/2004/02/skos/core#exactMatch> <%s> . # %s" % (e1, e2, probability))
        else:
            print("Unsupported " + cell.find("{http://knowledgeweb.semanticweb.org/heterogeneity/alignment}property").text)


if __name__ == "__main__":
    main()
