package org.insightcentre.uld.naisc.feature.wordnet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parse for reading the Global WordNet Association XML format
 * @author John McCrae
 */
public class GWNWordNetReader extends DefaultHandler {
    private LexicalEntry currentEntry;
    private String currentEntryId;
    private Synset currentSynset;
    private String currentSynsetId;
    private Sense currentSense;
    private boolean inDefn;
    private WordNetData wordnet = new WordNetData();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if(qName.equals("LexicalEntry")) {
            currentEntryId = atts.getValue("id");
        } else if(qName.equals("Lemma")) {
            currentEntry = new LexicalEntry(atts.getValue("writtenForm"), atts.getValue("partOfSpeech"), Collections.EMPTY_LIST);
        } else if(qName.equals("Sense")) {
            currentSense = new Sense(atts.getValue("id"), atts.getValue("synset"), Collections.EMPTY_LIST);
        } else if(qName.equals("SenseRelation")) {
            List<Sense.Relation> relations = new LinkedList<>();
            relations.addAll(currentSense.relations);
            relations.add(new Sense.Relation(atts.getValue("relType"), atts.getValue("target")));
            currentSense = new Sense(currentSense.id, currentSense.synset, relations);
        } else if(qName.equals("Synset")) {
            currentSynsetId = atts.getValue("id");
        } else if(qName.equals("Definition")) {
            inDefn = true;
        } else if(qName.equals("SynsetRelation")) {
            List<Synset.Relation> relations = new LinkedList<>();
            relations.addAll(currentSynset.relations);
            relations.add(new Synset.Relation(atts.getValue("relType"), atts.getValue("target")));
            currentSynset = new Synset(currentSynsetId, currentSynset.definition, relations);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(qName.equals("LexicalEntry")) {
            wordnet.addEntry(currentEntryId, currentEntry);
            currentEntryId = null;
            currentEntry = null;
        } else if(qName.equals("Sense")) {
            List<Sense> senses = new LinkedList<>();
            senses.addAll(currentEntry.senses);
            senses.add(currentSense);
            currentEntry = new LexicalEntry(currentEntry.writtenFrom, currentEntry.partOfSpeech, senses);
        } else if(qName.equals("Synset")) {
            wordnet.addSynset(currentSynsetId, currentSynset);
            currentSynsetId = null;
            currentSynset = null;
        } else if(qName.equals("Definition")) {
            inDefn = false;
        }
    }

    @Override
    public void characters(char[] arg0, int start, int length) throws SAXException {
        if(inDefn) {
            String definition = (currentSynset == null ? "" : currentSynset.definition) + 
                new String(arg0, start, length);
            currentSynset = new Synset(currentSynsetId, definition, Collections.EMPTY_LIST);
        }
    }
   
    public static WordNetData readFile(File file) throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        GWNWordNetReader handler = new GWNWordNetReader();
        reader.setContentHandler(handler);
        if(file.getName().endsWith(".gz")) {
            reader.parse(new InputSource(new GZIPInputStream(new FileInputStream(file))));
        } else {
            reader.parse(new InputSource(new FileInputStream(file)));
        }
        return handler.wordnet;
    }

    public static WordNetData readString(String data) throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        GWNWordNetReader handler = new GWNWordNetReader();
        reader.setContentHandler(handler);
        reader.parse(new InputSource(new StringReader(data)));
        return handler.wordnet;
    }


        
}
