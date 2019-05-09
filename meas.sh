#!/bin/bash

if [ ! -f naisc-meas/target/naisc-meas-jar-with-dependencies.jar ]
then
    mvn -q -f naisc-core/pom.xml install
    mvn -q -f naisc-meas/pom.xml package assembly:single
fi
java -jar naisc-meas/target/naisc-meas-jar-with-dependencies.jar "$@"
