#!/bin/bash

# Change to directory where this script is located
cd "$(dirname "$0")"

# If the clean flag is set then clear the JAR
if [ $1 = "--clean" ]
then
    rm -fr naisc-meas/target
    shift
fi

# If the JAR is not build then build
if [ ! -f naisc-meas/target/naisc-meas-jar-with-dependencies.jar ]
then
    mvn -q -f naisc-core/pom.xml install
    mvn -q -f naisc-meas/pom.xml package assembly:single
fi

# Run Naisc Meas
java -jar naisc-meas/target/naisc-meas-jar-with-dependencies.jar "$@"
