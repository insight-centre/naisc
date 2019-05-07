# NAISC - Automated Linking Tool

_'Naisc' means 'links' in Irish and is pronounced 'nashk'._

## Installation

Naisc can be installed with Maven, to compile and run the system run the 
following:

    ./meas.sh

The Web interface will be available at `http://localhost:8080`


## Command line operation

Naisc can be operated from the command line with the following script

    ./naisc.sh left.rdf right.rdf -c config.json -o alignment.rdf

This will output the alignment using the configuration to `alignment.rdf`

## Basic configurations

The following basic configurations are available:

1. `config/jaccard.json`: A simple Jaccard based string similarity
2. `config/simple.json`: Uses string similarity metrics only

## Documentation

Javadoc for Naisc is available at https://uld.pages.insight-centre.org/naisc

There is an overview of the tool available [here](https://docs.google.com/presentation/d/1bWThA0umgkZY1CcUguNTHspNQQt3tAvOMKUaS2i0M-U/edit?usp=sharing)
