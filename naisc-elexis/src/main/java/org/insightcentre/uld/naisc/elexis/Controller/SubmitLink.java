package org.insightcentre.uld.naisc.elexis.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.jena.rdf.model.Model;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.elexis.Model.MessageBody;
import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRest;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.ExecuteListener;
import org.insightcentre.uld.naisc.util.None;
import org.json.JSONException;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sampritha Manjunath
 */
@RestController
public class SubmitLink {
    static Model model;
    static ExecuteListener listener;
    static AlignmentSet alignmentSet;


    @GetMapping("/")
    public List<String> test() throws MalformedURLException, JSONException {
        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        ELEXISRest elexisRest = new ELEXISRest(endpoint);
        return elexisRest.getDictionaries();
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
//        Test connection string
        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
//        URL endpoint = new URL(messageBody.getSource().getEndpoint());
        ELEXISRest elexisRest = new ELEXISRest(endpoint);

        listener.updateStatus(NaiscListener.Stage.INITIALIZING, "Linking dictionaries");
//        Test
//        modelObj = elexisRest.getEntryAsTurtle("dictionary","entry1");
        List<Model> modelObj = new ArrayList<Model>();
        List<String> entries = messageBody.getSource().getEntries();
        for(int i = 0; i < entries.size() ; i++)
        {
            modelObj.add(i, elexisRest.getEntryAsTurtle(messageBody.getSource().getId(), entries.get(i)));
        }
        Configuration config = messageBody.getConfiguration().getSome();
//        alignmentSet = org.insightcentre.uld.naisc.main.Main.execute("uniqueID", null, null,
//                config, new None<>(), listener, new DefaultDatasetLoader() );
        alignmentSet = org.insightcentre.uld.naisc.main.Main.execute("name1", null, null, config,
                new None<>(), listener, new DatasetLoader() {
                    @Override
                    public Dataset fromFile(File file, String name) throws IOException {
                        return null;
                    }

                    @Override
                    public Dataset fromEndpoint(URL endpoint) {
                        return null;
                    }

                    @Override
                    public Dataset combine(Dataset dataset1, Dataset dataset2, String name) {
                        return null;
                    }
                });
        return modelObj;
    }

    @GetMapping("/status")
    public String getLinkingStatus()
    {
        return listener.toString();
    }


    @GetMapping("/result")
    public AlignmentSet getResults()
    {
        return alignmentSet;
    }
}