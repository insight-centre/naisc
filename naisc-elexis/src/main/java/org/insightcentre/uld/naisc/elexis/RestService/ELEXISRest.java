package org.insightcentre.uld.naisc.elexis.RestService;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Class to access the data from ELEXIS REST APIs
 * defined at: https://elexis-eu.github.io/elexis-rest/
 *
 * @author Suruchi Gupta
 */
public class ELEXISRest {
    private static URL endpoint;
    private static APIConnection apiConnection;

    private static String XSL_FILEPATH = "src/main/java/elexis/rest/service/TEI2Ontolex.xsl";
    private static final String XML_START = "<TEI version=\"3.3.0\" xmlns=\"http://www.tei-c.org/ns/1.0\"> " +
            "<teiHeader> </teiHeader> <text> <body>";
    private static final String XML_END = "</body> </text> </TEI>";

    private static final String ONTOLEX_HEADER = "http://www.w3.org/ns/lemon/ontolex#";
    private static final String SKOS_HEADER = "http://www.w3.org/2004/02/skos/core#";
    private static final String LEXINFO_HEADER = "http://www.lexinfo.net/ontology/2.0/lexinfo#";

    /**
     * Creating a new object
     *
     * @param endpoint
     */
    public ELEXISRest(URL endpoint) {
        ELEXISRest.endpoint = endpoint;
        apiConnection = new APIConnection(endpoint);
    }

    /**
     * Calls dictionaries endpoint and returns list of available dictionaries
     *
     * @return List of all dictionaries available
     */
    public List<String> getDictionaries() throws MalformedURLException {
        URL dictEndpoint = new URL(endpoint.toString()+"/dictionaries");
        String response = apiConnection.executeAPICall(dictEndpoint);

        JSONObject jsonResponse = new JSONObject(response);

        ArrayList<String> dictionaries = new ArrayList<String>();
        if(jsonResponse.has("dictionaries")) {
            JSONArray dictArray = (JSONArray) jsonResponse.get("dictionaries");
            dictArray.forEach(dict -> dictionaries.add((String) dict));
        }
        return dictionaries;
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
        URL aboutDictEndPoint = new URL(endpoint.toString()+"/about/"+dictionary);
        String response = apiConnection.executeAPICall(aboutDictEndPoint);

        ObjectMapper objectMapper = new ObjectMapper();
        MetaData metaData = objectMapper.readValue(response, MetaData.class);

        return metaData;
    }

    /**
     * Returns all the lemmas in the given dictionary
     *
     * @param dictionary
     * @return List of Lemmas
     * @throws MalformedURLException
     */
    public Lemma[] getAllLemmas(String dictionary) throws MalformedURLException, JsonProcessingException {
        URL allLemmasEndPoint = new URL(endpoint.toString()+"/list/"+dictionary);
        String response = apiConnection.executeAPICall(allLemmasEndPoint);

        if(response == null) {
            throw new RuntimeException("Failed to get a response from " + endpoint.toString() + " perhaps it has not started?");
        }
        ObjectMapper objectMapper = new ObjectMapper();
        Lemma[] allLemmas = objectMapper.readValue(response, Lemma[].class);

        return allLemmas;
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
        URL headWordsEndPoint = new URL(endpoint.toString()+"/lemma/"+dictionary+"/"+headword);
        String response = apiConnection.executeAPICall(headWordsEndPoint);

        ObjectMapper objectMapper = new ObjectMapper();
        Lemma[] allHeadWords = objectMapper.readValue(response, Lemma[].class);

        return allHeadWords;
    }

    /**
     * Returns the entry in the dictionary in form of JSONObject
     *
     * @param dictionary
     * @param id
     * @return dictionary entry As JSON
     * @throws MalformedURLException
     */
    public JSONObject getEntryAsJSON(String dictionary, String id) throws MalformedURLException {
        URL entryAsJSONEndPoint = new URL(endpoint.toString()+"/json/"+dictionary+"/"+id);
        String response = apiConnection.executeAPICall(entryAsJSONEndPoint);

        JSONObject entryAsJSON = new JSONObject(response);
        return entryAsJSON;
    }

    /**
     * Returns OntoLex model for the input string
     *
     * @param inputString
     * @return OntoLex Model
     */
    public Model parseTurtleEntry(String inputString) {
        Model turtleModel = ModelFactory.createDefaultModel();
        turtleModel.read(new ByteArrayInputStream(inputString.getBytes()), null, "TTL");
        return turtleModel;
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
        URL entryAsTurtleEndPoint = new URL(endpoint.toString()+"/ontolex/"+dictionary+"/"+id);
        String response = apiConnection.executeAPICall(entryAsTurtleEndPoint);

        Model entryAsTurtle = parseTurtleEntry(response);
        return entryAsTurtle;
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
        URL entryAsTEIEndPoint = new URL(endpoint.toString()+"/tei/"+dictionary+"/"+id);
        String response = apiConnection.executeAPICall(entryAsTEIEndPoint);

        // Appending the start and end XML tags for proper transformation
        response = XML_START+response+XML_END;

        // Using the xsl file to process the API response
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xsl = new StreamSource(new File(XSL_FILEPATH));
        Source text = new StreamSource(new StringReader(response));
        Transformer transformer = factory.newTransformer(xsl);

        // Reading the transformed XML
        StringWriter outWriter = new StringWriter();
        transformer.transform(text, new StreamResult(outWriter));
        String finalResponse = outWriter.getBuffer().toString();

        // Converting the transformed XML into RDF Model
        Model entryAsTEI = ModelFactory.createDefaultModel();
        entryAsTEI.read(new ByteArrayInputStream(finalResponse.getBytes()), null, "RDF/XML");

        return entryAsTEI;
    }

    /**
     * Parses the input JSON object and creates an equivalent RDF model
     *
     * @param jsonObject
     * @return converted RDF model
     */
    public Model jsonToRDF(JSONObject jsonObject) {

        // Creating default model and adding prefixes
        Model rdfModel = ModelFactory.createDefaultModel();
        rdfModel.setNsPrefix("lexinfo", LEXINFO_HEADER);
        rdfModel.setNsPrefix("skos", SKOS_HEADER);
        rdfModel.setNsPrefix("ontolex", ONTOLEX_HEADER);

        // Getting the id and creating basic RDF model
        String id = "#" + jsonObject.getString("@id");
        rdfModel.createResource(id).addProperty(RDF.type, rdfModel.createResource(ONTOLEX_HEADER + "LexicalEntry"));

        // Appending partOfSpeech details
        String partOfSpeech = jsonObject.get("partOfSpeech").toString();
        Resource posResource = rdfModel.createResource(LEXINFO_HEADER+partOfSpeech);
        Property posProperty = rdfModel.createProperty(LEXINFO_HEADER, "partOfSpeech");
        rdfModel.getResource(id).addProperty(posProperty, posResource);

        // Adding canonicalForm(writtenRep + phoneticRep) details
        Resource canonicalFormNode = rdfModel.createResource();
        JSONObject canonicalForm = jsonObject.getJSONObject("canonicalForm");
        String writtenRep = canonicalForm.get("writtenRep").toString();
        String language = "";
        if(jsonObject.has("language"))
           language = jsonObject.getString("language");

        Property writtenRepProperty = rdfModel.createProperty(ONTOLEX_HEADER, "writtenRep");
        Literal writtenRepLiteral = rdfModel.createLiteral(writtenRep, language);
        canonicalFormNode.addProperty(writtenRepProperty, writtenRepLiteral);

        if(canonicalForm.has("phoneticRep")) {
            String phoneticRep = canonicalForm.get("phoneticRep").toString();
            Property phoneticRepProperty = rdfModel.createProperty(ONTOLEX_HEADER, "phoneticRep");
            Literal phoneticRepLiteral = rdfModel.createLiteral(phoneticRep);
            canonicalFormNode.addProperty(phoneticRepProperty, phoneticRepLiteral);
        }
        rdfModel.getResource(id).addProperty(rdfModel.createProperty(ONTOLEX_HEADER, "canonicalForm"), canonicalFormNode);

        // Adding senses(definition + reference) details
        Resource sensesNode = rdfModel.createResource();
        JSONArray senses = jsonObject.getJSONArray("senses");
        for (Object s: senses) {
            JSONObject sense = (JSONObject) s;
            for (Object k : sense.keySet()) {
                String key = (String) k;
                if(key.equals("definition")) {
                    Property definitionProperty = rdfModel.createProperty(SKOS_HEADER, "definition");
                    Literal definitionLiteral = rdfModel.createLiteral(sense.get(key).toString(), language);
                    sensesNode.addProperty(definitionProperty, definitionLiteral);
                } else if(key.equals("reference")) {
                    Property referenceProperty = rdfModel.createProperty(ONTOLEX_HEADER, "reference");
                    Literal referenceLiteral = rdfModel.createLiteral(sense.get(key).toString());
                    sensesNode.addProperty(referenceProperty, referenceLiteral);
                }
            }
        }
        rdfModel.getResource(id).addProperty(rdfModel.createProperty(ONTOLEX_HEADER, "sense"), sensesNode);

        return rdfModel;
    }
}
