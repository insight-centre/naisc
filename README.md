# NAISC - Automated Linking Tool

_'Naisc' means 'links' in Irish and is pronounced 'nashk'._

![Naisc Logo](https://github.com/insight-centre/naisc/raw/dev/Naisc%20Logo.png)

## Installation

Naisc can be installed with Gradle, to compile and run the system run the 
following:

    ./gradlew jarWithDeps

## Meas - Meas Evaluation and Analysis Suite

For developing models and training there is a web application that can be built
by the following

    ./gradlew meas

The Web interface will be available at `http://localhost:8080`

## ELEXIS Interface

The linking interface for use with ELEXIS can be started with

    ./gradlew bootRun

It will be available at `http://localhost:8080`. More information about how
to use this service is available at https://elexis-eu.github.io/elexis-rest/linking.html

## Command line operation

Naisc can be operated from the command line with the following script

    ./naisc.sh left.rdf right.rdf -c config.json -o alignment.rdf

This will output the alignment using the configuration to `alignment.rdf`

Offline training can be created using the training script, the dataset should 
be available under `datasets/`

    ./train.sh dataset -c config.json

### Command line options

**For linking (`naisc.sh`)**

    Option       Description
    ------       -----------
    -c <File>    The configuration to use
    -f <File>    Dump features
    -n <Double>  Negative Sampling rate (number of
                   negative examples/positive example)
    -q           Quiet (suppress output)

**For training (`train.sh`)**

    Option       Description
    ------       -----------
    -c <File>    The configuration to use
    -f <File>    Dump features
    -n <Double>  Negative Sampling rate (number of
                   negative examples/positive example)
    -q           Quiet (suppress output)

## Basic configurations

The following basic configurations are available:

1. `config/jaccard.json`: A simple Jaccard based string similarity
2. `config/string-match.json`: Uses string similarity metrics only
3. `config/auto.json`: The general purpose linker

## Documentation

Javadoc for Naisc is available at https://uld.pages.insight-centre.org/naisc

There is an overview of the tool available [here](https://docs.google.com/presentation/d/1bWThA0umgkZY1CcUguNTHspNQQt3tAvOMKUaS2i0M-U/edit?usp=sharing)

For extending Naisc with new services please see the guide [here](EXTENDING.md)

There is a quick video introduction on YouTube:

[![Watch an introduction to Naisc](https://img.youtube.com/vi/maYEv8rG0_k/0.jpg)](https://www.youtube.com/watch?v=maYEv8rG0_k)



