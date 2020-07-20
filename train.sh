#!/bin/bash

# Change to directory where this script is located
cd "$(dirname "$0")"

./gradlew jarWithDeps

java -cp naisc-core/build/libs/naisc-core-all-1.0-SNAPSHOT.jar org.insightcentre.uld.naisc.main.MultiTrain $*
