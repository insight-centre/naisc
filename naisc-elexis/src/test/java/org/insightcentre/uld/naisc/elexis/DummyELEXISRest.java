package org.insightcentre.uld.naisc.elexis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRest;
import org.insightcentre.uld.naisc.elexis.RestService.Lemma;
import org.insightcentre.uld.naisc.elexis.RestService.MetaData;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DummyELEXISRest extends ELEXISRest {
    private final String dictionaryName;
    private final List<String> entries;
    private final List<List<String>> definitions;
    private static final String ONTOLEX_HEADER = "http://www.w3.org/ns/lemon/ontolex#";
    private static final String SKOS_HEADER = "http://www.w3.org/2004/02/skos/core#";
    private static final String LEXINFO_HEADER = "http://www.lexinfo.net/ontology/2.0/lexinfo#";
    public String boobyTrap = null;

    public DummyELEXISRest(URL endpoint, String dictionaryName, List<String> entries, List<List<String>> definitions) {
        super(endpoint);
        this.dictionaryName = dictionaryName;
        this.entries = entries;
        this.definitions = definitions;
    }

    public List<String> getDictionaries() throws MalformedURLException {
        return Collections.singletonList(dictionaryName);
    }

    /**
     * Fetches the elexis.rest.service.MetaData of the provided dictionary
     *
     * @param dictionary
     * @return elexis.rest.service.MetaData
     * @throws MalformedURLException
     * @throws JsonProcessingException
     */
    public MetaData aboutDictionary(String dictionary) throws MalformedURLException, JsonProcessingException {
        MetaData md = new MetaData();
        return md;
    }

    /**
     * Returns all the lemmas in the given dictionary
     *
     * @param dictionary
     * @return List of Lemmas
     * @throws MalformedURLException
     */
    public Lemma[] getAllLemmas(String dictionary) throws MalformedURLException, JsonProcessingException {
        List<Lemma> lemmaList = entries.stream().map(e -> {
            Lemma l = new Lemma();
            l.setId("id-" + e);
            l.setLemma(e);
            return l;
        }).collect(Collectors.toList());
        return lemmaList.toArray(new Lemma[entries.size()]);
    }

    /**
     * Returns list of entries for the given headword in a dictionary
     *
     * @param dictionary
     * @param headword
     * @return List of entries under the given headword
     * @throws JsonProcessingException
     * @throws MalformedURLException
     */
    public Lemma[] getHeadWordLookup(String dictionary, String headword) throws JsonProcessingException, MalformedURLException {
        if(entries.contains(headword)) {
            Lemma l = new Lemma();
            l.setId("id-" + headword);
            l.setLemma(headword);
            return new Lemma[] { l };
        } else {
            return new Lemma[] {};
        }
    }

    /**
     * Returns the entry in the dictionary in form of JSONObject
     *
     * @param dictionary The dictionary
     * @param entryId The ID of the entry
     * @return dictionary entry As JSON
     * @throws MalformedURLException
     */
    public Model getEntryAsJSON(String dictionary, String entryId) throws MalformedURLException {
        Model rdfModel = ModelFactory.createDefaultModel();
        rdfModel.setNsPrefix("lexinfo", LEXINFO_HEADER);
        rdfModel.setNsPrefix("skos", SKOS_HEADER);
        rdfModel.setNsPrefix("ontolex", ONTOLEX_HEADER);

        // Getting the id and creating basic RDF model
        String id = "#" + entryId;
        rdfModel.createResource(id).addProperty(RDF.type, rdfModel.createResource(ONTOLEX_HEADER + "LexicalEntry"));

        // Appending partOfSpeech details
        String partOfSpeech = "noun";
        Resource posResource = rdfModel.createResource(LEXINFO_HEADER+partOfSpeech);
        Property posProperty = rdfModel.createProperty(LEXINFO_HEADER, "partOfSpeech");
        rdfModel.getResource(id).addProperty(posProperty, posResource);

        // Adding canonicalForm(writtenRep + phoneticRep) details
        Resource canonicalFormNode = rdfModel.createResource();
        String writtenRep = entryId.substring(3);
        if(writtenRep.equals(boobyTrap)) {
            throw new RuntimeException("Test is not supposed to call this!");
        }
        int index = entries.indexOf(writtenRep);
        String language = "en";

        Property writtenRepProperty = rdfModel.createProperty(ONTOLEX_HEADER, "writtenRep");
        Literal writtenRepLiteral = rdfModel.createLiteral(writtenRep, language);
        canonicalFormNode.addProperty(writtenRepProperty, writtenRepLiteral);

        rdfModel.getResource(id).addProperty(rdfModel.createProperty(ONTOLEX_HEADER, "canonicalForm"), canonicalFormNode);

        // Adding senses(definition + reference) details
        int i = 0;
        for (String s : definitions.get(index)) {
                Resource sensesNode = rdfModel.createResource(id + "-sense-" + ++i);
                Property definitionProperty = rdfModel.createProperty(SKOS_HEADER, "definition");
                Literal definitionLiteral = rdfModel.createLiteral(s, language);
                sensesNode.addProperty(definitionProperty, definitionLiteral);
                rdfModel.getResource(id).addProperty(rdfModel.createProperty(ONTOLEX_HEADER, "sense"), sensesNode);
        }

        return rdfModel;
    }

    /**
     * Returns the entry in the dictionary in form of RDF
     *
     * @param dictionary
     * @param id
     * @return dictionary entry as RDF
     * @throws MalformedURLException
     */
    public Model getEntryAsTurtle(String dictionary, String id) throws MalformedURLException {
        return getEntryAsJSON(dictionary, id);
    }

    /**
     * Returns the entry in the dictionary in form of TEI document
     *
     * @param dictionary
     * @param id
     * @return dictionary entry as TEI
     * @throws MalformedURLException
     * @throws TransformerException
     */
    public Model getEntryAsTEI(String dictionary, String id) throws MalformedURLException, TransformerException {
        return getEntryAsJSON(dictionary, id);
    }
}
