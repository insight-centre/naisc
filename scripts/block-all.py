## This is an example of how an external Python script can be called from 
## Naisc. This is identical to the 'All' blocking strategy of Naisc
## This is intended to be run as blocking command with the `command` as
##   python3 scripts/block-all.py $SPARQL_LEFT $SPARQL_RIGHT
import sys
import json
import urllib.request

left_endpoint = sys.argv[1]
right_endpoint = sys.argv[2]

lefts = set()
rights = set()

with urllib.request.urlopen(left_endpoint + "?query=SELECT%20?s%20WHERE%20{%20?s%20?p%20?o%20}&format=application/sparql-results%2bjson") as response:
   data = json.loads(response.read())
   for result in data["results"]["bindings"]:
       if result["s"]["type"] == "uri":
           lefts.add(result["s"]["value"])

with urllib.request.urlopen(right_endpoint + "?query=SELECT%20?s%20WHERE%20{%20?s%20?p%20?o%20}&format=application/sparql-results%2bjson") as response:
   data = json.loads(response.read())
   for result in data["results"]["bindings"]:
       if result["s"]["type"] == "uri":
           rights.add(result["s"]["value"])



for l in lefts:
    for r in rights:
        print(l + "\t" + r)

