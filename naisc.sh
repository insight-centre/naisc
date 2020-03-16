#!/bin/bash

# Change to directory where this script is located
cd "$(dirname "$0")"

./gradlew jarWithDeps

java -jar naisc-core/build/libs/naisc-core-all-1.0-SNAPSHOT.jar $*
