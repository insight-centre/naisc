package org.insightcentre.uld.naisc.elexis.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.AlignmentSet;

import org.insightcentre.uld.naisc.elexis.Model.*;
import org.insightcentre.uld.naisc.elexis.Model.MessageBody;

import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRest;
import org.insightcentre.uld.naisc.elexis.RestService.Lemma;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
import org.insightcentre.uld.naisc.util.None;
import org.json.JSONException;
import org.springframework.web.bind.annotation.*;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Implementation of ELEXIS Linking APIs to return the linking between two dictionaries
 * defined at: https://elexis-eu.github.io/elexis-rest/linking.html#
 *
 * @author Sampritha Manjunath
 */
@RestController
public class SubmitLink {
    private AlignmentSet alignmentSet;

    /**
     * Default call to the ELEXIS Linking API
     * @return List of available dictionaries
     * @throws MalformedURLException
     * @throws JSONException
     */
    @GetMapping("/")
    public DefaultResponse test() throws MalformedURLException, JSONException {
        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        ELEXISRest elexisRest = new ELEXISRest(endpoint);

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setDictionaries((ArrayList<String>) elexisRest.getDictionaries());
        return defaultResponse;
    }

    /**
     * API call to link the sense of two dictionaries
     * @param messageBody
     * @return requestID for the request
     * @throws JSONException
     * @throws IOException
     */
    @PostMapping(value = "/submit")
    public SubmitResponse submitLinkRequest(@RequestBody MessageBody messageBody)
            throws IOException, TransformerException {

        // Generating uniqueID for each submit request
        String uniqueID = UUID.randomUUID().toString();

        org.insightcentre.uld.naisc.main.Configuration config = null;
        if(config == null) {
            // Reading default configuration details
            config = new ObjectMapper().readValue(new File("configs/auto.json"), Configuration.class);
        } else {
            // Setting the configurations sent in the MessageBody
            config = messageBody.getConfiguration().getSome();
        }

        // Processing source object from MessageBody
        URL sourceEndpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        if(null != messageBody.getSource().getEndpoint()) {
            sourceEndpoint = new URL(messageBody.getSource().getEndpoint());
        }
        ELEXISRest elexisRest = new ELEXISRest(sourceEndpoint);

        // Getting the left dictionary id and the entries from messageBody
        String sourceId = messageBody.getSource().getId();
        ArrayList<String> sourceEntries = messageBody.getSource().getEntries();
        Model leftModel = processDictionary(sourceEntries, elexisRest, sourceId);

        // Writing the left dictionary into leftFile
        File leftFile = new File("leftFile.ttl");
        FileWriter out = new FileWriter(leftFile);
        leftModel.write( out, "TTL" );

        // Processing target object from MessageBody
        URL targetEndpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        if(null != messageBody.getTarget().getEndpoint()) {
            targetEndpoint = new URL(messageBody.getTarget().getEndpoint());
        }
        elexisRest = new ELEXISRest(targetEndpoint);

        // Getting the right dictionary id and the entries from messageBody
        String targetId = messageBody.getTarget().getId();
        ArrayList<String> targetEntries = messageBody.getTarget().getEntries();
        Model rightModel = processDictionary(targetEntries, elexisRest, targetId);

        // Writing the right dictionary into rightFile
        File rightFile = new File("rightFile.ttl");
        out = new FileWriter(rightFile);
        rightModel.write( out, "TTL" );


        // Calling the execute method with the generated files
        // TODO - Making the call Async - WIP
        alignmentSet = org.insightcentre.uld.naisc.main.Main.execute(uniqueID, leftFile, rightFile,
                config, new None<>(), ExecuteListeners.STDERR, new DefaultDatasetLoader());

//      TODO - Direct execute call not working - need to check
//        alignmentSet = org.insightcentre.uld.naisc.main.Main.execute(uniqueID, leftModel, rightModel,
//                config, new None<>(), listener, null, null, new DefaultDatasetLoader());

        // Returning the requestId for the request
        SubmitResponse submitResponse = new SubmitResponse();
        submitResponse.setRequestId(uniqueID);
        return submitResponse;
    }

    /**
     * Method to return all the valid entries for linking as Model
     * @param entries
     * @param elexisRest
     * @param id
     * @return
     * @throws IOException
     * @throws TransformerException
     */
    private Model processDictionary(ArrayList<String> entries, ELEXISRest elexisRest,
                                   String id) throws IOException, TransformerException {

        // Getting all the lemmas from the dictionary and creating a map
        Lemma[] lemmas = elexisRest.getAllLemmas(id);
        HashMap<String,Lemma> map = new HashMap<String,Lemma>();
        for (Lemma l : lemmas)
            map.put(l.getId(),l);

        // Creating a model and retrieving the relevant entries
        Model model = ModelFactory.createDefaultModel();
        if(null != entries && entries.size() != 0) {
            for(String entry : entries) {
                model.add(getEntry(map, elexisRest, id, entry));
            }
        } else {
            for(Lemma l : lemmas) {
                model.add(getEntry(map, elexisRest, id, l.getId()));
            }
        }
        return model;
    }

    /**
     * Method to return the lemma as model based on the available format
     * @param map
     * @param elexisRest
     * @param id
     * @param entry
     * @return
     * @throws MalformedURLException
     * @throws TransformerException
     */
    private Model getEntry(HashMap<String, Lemma> map, ELEXISRest elexisRest, String id, String entry) throws MalformedURLException, TransformerException {
        String formats = map.get(entry).getFormats().toString().toLowerCase();

        // Check the format available and return the model for the entry
        if(formats.contains("ontolex")) {
             return elexisRest.getEntryAsTurtle(id, entry);
        } else if(formats.contains("tei")) {
            return elexisRest.getEntryAsTEI(id, entry);
        } else {
            return elexisRest.jsonToRDF(elexisRest.getEntryAsJSON(id, entry));
        }
    }

    /**
     * API call to retrieve the status of linking
     * @return
     */
    @PostMapping(value = "/status")
    public String getLinkStatus()
    {
        // TODO - WIP
        return "";
    }

    /**
     * API call to return the linking response
     * @return
     */
    @PostMapping("/result")
    public AlignmentSet getResults()
    {
        //TODO - Format response
        return alignmentSet;
    }
}