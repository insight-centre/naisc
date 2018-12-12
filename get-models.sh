#!/bin/bash

MODELS=models
mkdir $MODELS
if [ ! -f $MODELS/saliency ]
then
    wget http://john.mccr.ae/naisc/saliency -O $MODELS/saliency
fi
if [ ! -f $MODELS/ppdb ]
then
    wget http://john.mccr.ae/naisc/ppdb -O $MODELS/ppdb
fi
if [ ! -f $MODELS/idf ]
then
    wget http://john.mccr.ae/naisc/idf -O $MODELS/idf
fi
if [ ! -f $MODELS/ngidf ]
then
    wget http://john.mccr.ae/naisc/ngidf -O $MODELS/ngidf
fi
if [ ! -f $MODELS/wiki ]
then
    wget http://john.mccr.ae/wiki.en.gz -O $MODELS/wiki
fi
if [ ! -f glove.6B.zip ]
then
    wget http://nlp.stanford.edu/data/glove.6B.zip 
fi
unzip -d $MODELS glove.6B.zip
if [ ! -f stanford-english-corenlp-2016-01-10-models.jar ]
then
    wget http://nlp.stanford.edu/software/stanford-english-corenlp-2016-01-10-models.jar
fi
unzip -j -d $MODELS stanford-english-corenlp-2016-01-10-models.jar edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz edu/stanford/nlp/models/pos-tagger/english-bidirectional/english-bidirectional-distsim.tagger edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz

if [ ! -d $MODELS/stopwords ]
then
    wget https://raw.githubusercontent.com/nltk/nltk_data/gh-pages/packages/corpora/stopwords.zip
    unzip stopwords.zip -d $MODELS
    rm stopwords.zip
fi

if [ ! -f $MODELS/wn31.xml.gz ]
then
    wget http://john.mccr.ae/wn31.xml -O models/wn31.xml
    gzip models/wn31.xml
fi

if [ ! -f $MODELS/rnn-300 ]
then
    wget http://john.mccr.ae/naisc/rnns.zip -O models/rnns.zip
    gzip models/rnns.zip
fi

if [ ! -d data ]
then
    wget http://john.mccr.ae/naisc/data.zip
    unzip data.zip
    rm data.zip
fi
