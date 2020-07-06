package org.insightcentre.uld.naisc.elexis.Controller;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.AlignmentSet;

import org.insightcentre.uld.naisc.elexis.Model.*;
import org.insightcentre.uld.naisc.elexis.Model.MessageBody;

import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRest;
import org.insightcentre.uld.naisc.elexis.RestService.Lemma;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
import org.insightcentre.uld.naisc.util.None;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.parser.Entity;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * ELEXIS REST APIs for linking two dictionaries
 * defined at: https://elexis-eu.github.io/elexis-rest/linking.html#
 *
 * @author Suruchi Gupta
 */
@RestController
public class SubmitLink {
//    static Model model;
    static AlignmentSet alignmentSet;

    /**
     *
     * @return
     * @throws MalformedURLException
     * @throws JSONException
     */
    @GetMapping("/")
    public List<String> test() throws MalformedURLException, JSONException {
        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        ELEXISRest elexisRest = new ELEXISRest(endpoint);
        return elexisRest.getDictionaries();
    }

    @GetMapping("/test")
    public ArrayList<String> entriesTest()
    {
        ArrayList<String> entries = new ArrayList<String>();
        entries.add("cat");
        entries.add("dog");

        Source source = new Source();
        source.setEntries(entries);

        return source.getEntries();
    }

    /**
     *
     * @param messageBody
     * @return
     * @throws JSONException
     * @throws IOException
     */
    @PostMapping(value = "/submit")
    public String submitLinkRequest(@RequestBody MessageBody messageBody)
            throws IOException, TransformerException {
        // Generating uniqueID for each submit request
        String uniqueID = UUID.randomUUID().toString();

        // Setting the configurations sent in the MessageBody
        org.insightcentre.uld.naisc.main.Configuration config = null;
        if(null != messageBody.getConfiguration())
            config = messageBody.getConfiguration().getSome();

        // Processing source object from MessageBody
        URL sourceEndpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        if(null != messageBody.getSource().getEndpoint()) {
            sourceEndpoint = new URL(messageBody.getSource().getEndpoint());
        }
        ELEXISRest elexisRest = new ELEXISRest(sourceEndpoint);

        // Getting the left dictionary id and the entries from messageBody
        String sourceId = messageBody.getSource().getId();
        ArrayList<String> sourceEntries = messageBody.getSource().getEntries();
        File leftFile = processDictionary(sourceEntries, elexisRest, sourceId, "leftFile.rdf");

        // Processing target object from MessageBody
        URL targetEndpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        if(null != messageBody.getTarget().getEndpoint()) {
            targetEndpoint = new URL(messageBody.getTarget().getEndpoint());
        }
        elexisRest = new ELEXISRest(targetEndpoint);

        // Getting the left dictionary id and the entries from messageBody
        String targetId = messageBody.getTarget().getId();
        ArrayList<String> targetEntries = messageBody.getTarget().getEntries();
        File rightFile = processDictionary(targetEntries, elexisRest, targetId, "rightFile.rdf");

        // Calling the execute method with the generated files
        alignmentSet = org.insightcentre.uld.naisc.main.Main.execute(uniqueID, leftFile, rightFile,
                config, new None<>(), ExecuteListeners.NONE, new DefaultDatasetLoader());

        return uniqueID;
    }

    /**
     *
     * @param entries
     * @param elexisRest
     * @param id
     * @param filepath
     * @return
     * @throws IOException
     * @throws TransformerException
     */
    private File processDictionary(ArrayList<String> entries, ELEXISRest elexisRest,
                                   String id, String filepath) throws IOException, TransformerException {
        Lemma[] lemmas = elexisRest.getAllLemmas(id);
        HashMap<String,Lemma> map = new HashMap<String,Lemma>();
        for (Lemma l : lemmas)
            map.put(l.getId(),l);

        Model model = ModelFactory.createDefaultModel();
        if(entries.size() != 0) {
            for(String entry : entries) {
                model.add(getEntry(map, elexisRest, id, entry));
            }
        } else {
            for(Lemma l : lemmas) {
                model.add(getEntry(map, elexisRest, id, l.getId()));
            }
        }
        File file = new File(filepath);
        FileWriter out = new FileWriter(file);
        model.write( out, "TTL" );

        return file;
    }

    /**
     *
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
     *
     * @return
     */
    @PostMapping(value = "/status")
    public ResponseEntity getLinkStatus()
    {
        List<JSONObject> entities = new ArrayList<JSONObject>();
        Entity[] entityList = new Entity[0];
        for (Entity n : entityList) {
            JSONObject status = new JSONObject();
            status.put("state", "processing");
            status.put("message", "Still working away");
            entities.add(status);
        }
        return new ResponseEntity<Object>(entities, HttpStatus.OK);
    }

    /**
     *
     * @return
     */
    @PostMapping("/result")
    public ResponseEntity<Result> getResults()
    {
        Result createResult = new Result();
        Linking linking = new Linking();
        Source source = new Source();
        Target target = new Target();
        ArrayList<String> entries = source.getEntries();
        ArrayList<String> targetEntries = target.getEntries();
        List<Linking> linkList = new ArrayList<>();
        for(int i = 0; i < entries.size() ; i++)
        {
            createResult.setSourceEntry(entries.get(i));
            createResult.setTargetEntry(targetEntries.get(i));

            linking.setSourceSense("");
            linking.setTargetSense("");
            linking.setScore(0.0);
            linking.setType("");

            linkList.add(linking);
            createResult.setLinking(linkList);
        }

        return new ResponseEntity<Result>(HttpStatus.OK);
    }
}