package org.insightcentre.uld.naisc.elexis.RestService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.Command;
import org.apache.jena.shared.Lock;
import org.apache.jena.shared.PrefixMapping;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

/**
 * Class to access the data from ELEXIS REST APIs
 * defined at: https://elexis-eu.github.io/elexis-rest/
 *
 * @author Suruchi Gupta
 */
public class ELEXISRest {
    private static URL endpoint;
    APIConnection apiConnection;

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
    public JSONObject getEntryAsJSON(String dictionary, String id) throws MalformedURLException, JSONException {
        URL entryAsJSONEndPoint = new URL(endpoint.toString()+"/json/"+dictionary+"/"+id);
        String response = apiConnection.executeAPICall(entryAsJSONEndPoint);

        JSONObject entryAsJSON = new JSONObject(response);
        return entryAsJSON;
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
        URL entryAsJSONEndPoint = new URL(endpoint.toString()+"/ontolex/"+dictionary+"/"+id);
        String response = apiConnection.executeAPICall(entryAsJSONEndPoint);

        Model entryAsTurtle = ModelFactory.createDefaultModel();
        entryAsTurtle.read(new ByteArrayInputStream(response.getBytes()), null, "TTL");

        return entryAsTurtle;
    }
}