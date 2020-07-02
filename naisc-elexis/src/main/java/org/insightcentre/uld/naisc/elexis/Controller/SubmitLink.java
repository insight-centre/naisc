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
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
import org.insightcentre.uld.naisc.main.ExecuteListener;
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
    static ExecuteListener listener;
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
    public String[] entriesTest()
    {
        String[] entries = {"cat", "dog"};
        MessageBody messageBody = new MessageBody();
        Source source = new Source();
        source.setEntries(entries);

        return source.getEntries();
    }

    /**
     * @param messageBody
     * @return
     * @throws MalformedURLException
     * @throws JSONException
     * @throws JsonProcessingException
     */
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    public List<Model> submitLinkRequest(@RequestBody MessageBody messageBody) throws MalformedURLException, JSONException, JsonProcessingException {
//      Generate unique name for each submit request
        String uniqueID = UUID.randomUUID().toString();
//        Test connection string
        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019");
//        URL endpoint = new URL(messageBody.getSource().getEndpoint());
        ELEXISRest elexisRest = new ELEXISRest(endpoint);

        List<Model> modelObj = new ArrayList<Model>();

        String[] entries = messageBody.getSource().getEntries();
        for(int i = 0; i < entries.length ; i++)
        {
            modelObj.add(i, elexisRest.getEntryAsTurtle(messageBody.getSource().getId(), entries[i]));
        }

        org.insightcentre.uld.naisc.main.Configuration config = messageBody.getConfiguration().getSome();
        alignmentSet = org.insightcentre.uld.naisc.main.Main.execute("uniqueID", null, null,
                config, new None<>(), listener, new DefaultDatasetLoader() );

        return modelObj;
    }

    /**
     *
     * @return
     */
    @RequestMapping(value = "/status", method = RequestMethod.POST)
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
        String[] entries = source.getEntries();
        String[] targetEntries = Target.getEntries();
        List<Linking> linkList = new ArrayList<>();
        for(int i = 0; i < entries.length ; i++)
        {
            createResult.setSourceEntry(entries[i]);
            createResult.setTargetEntry(targetEntries[i]);

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