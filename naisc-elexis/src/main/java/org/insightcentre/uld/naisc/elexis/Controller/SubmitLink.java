package org.insightcentre.uld.naisc.elexis.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.jena.rdf.model.Model;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.elexis.Model.*;
import org.insightcentre.uld.naisc.elexis.Model.MessageBody;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRest;
import org.insightcentre.uld.naisc.main.*;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.util.None;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.parser.Entity;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Sampritha Manjunath
 */
@RestController
public class SubmitLink {
    static Model model;
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

        MessageBody messageBody = new MessageBody();
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
    public String submitLinkRequest(@RequestBody MessageBody messageBody) throws JSONException, IOException {
        // Generating uniqueID for each submit request
        String uniqueID = UUID.randomUUID().toString();


        // Getting the endpoint URL and creating ElexisRest object to retrieve the data
        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        if(null != messageBody.getSource().getEndpoint()) {
            endpoint = new URL(messageBody.getSource().getEndpoint());
        }
        ELEXISRest elexisRest = new ELEXISRest(endpoint);

        // Setting the configurations sent in the MessageBody
        org.insightcentre.uld.naisc.main.Configuration config = null;
        if(null != messageBody.getConfiguration())
            config = messageBody.getConfiguration().getSome();

        // Getting the nt filepath from messageBody
        String sourceId = messageBody.getSource().getId();
        String destinationId = messageBody.getSource().getId();

        File leftFile = elexisRest.readFile(sourceId, "leftFile.rdf");
        File rightFile = elexisRest.readFile(destinationId, "rightFile.rdf");

        // Checking the file formats available and reading the entries


        // Calling the execute method with the generated files
        alignmentSet = org.insightcentre.uld.naisc.main.Main.execute(uniqueID, leftFile, rightFile,
                config, new None<>(), ExecuteListeners.NONE, new DefaultDatasetLoader());

        return uniqueID;
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