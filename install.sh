#!/bin/bash

mkdir -p naisc-core/target
mkdir -p naisc-meas/target
curl https://github.com/insight-centre/naisc/releases/download/Ubuntu-latest/naisc-core-1.1-jar-with-dependencies.jar -o naisc-core/target/naisc-core-1.1-jar-with-dependencies.jar
curl https://github.com/insight-centre/naisc/releases/download/Ubuntu-latest/naisc-meas-jar-with-dependencies.jar -o naisc-meas/target/naisc-meas-jar-with-dependencies.jar
