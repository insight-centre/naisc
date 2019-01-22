# NAISC - Automated Linking Tool

_'Naisc' means 'links' in Irish and is pronounced 'nashk'._

## Installation

Naisc can be installed with Maven, to compile the core run

    mvn -f naisc-core/pom.xml install

To obtain the data necessary to run Naisc, run the following script

    bash get-models.sh

Finally, to run and start the Web interface use the following

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

Naisc documentation is available under `docs/`
