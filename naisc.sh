#!/bin/bash

# Change to directory where this script is located
cd "$(dirname "$0")"

if [ ! -f naisc-core/target/naisc-core-1.1-jar-with-dependencies.jar ]
then
    mvn install
fi

java -jar naisc-core/target/naisc-core-1.1-jar-with-dependencies.jar $*
