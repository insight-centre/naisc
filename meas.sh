#!/bin/bash

# Change to directory where this script is located
cd "$(dirname "$0")"

mvn -q exec:java -Dexec.mainClass="org.insightcentre.uld.naisc.meas.Launch" -Dexec.args="$*"
