#!/bin/bash

mvn -q -f naisc-core/pom.xml exec:java -Dexec.mainClass="org.insightcentre.uld.naisc.main.ExamineFeature" -Dexec.args="$*"
