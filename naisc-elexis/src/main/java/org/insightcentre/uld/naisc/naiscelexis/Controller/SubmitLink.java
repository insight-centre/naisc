package org.insightcentre.uld.naisc.naiscelexis.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.jena.rdf.model.Model;
import org.insightcentre.uld.naisc.naiscelexis.Model.MessageBody;
import org.insightcentre.uld.naisc.naiscelexis.RestService.ELEXISRest;
import org.insightcentre.uld.naisc.naiscelexis.RestService.Lemma;
import org.json.JSONException;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sampritha Manjunath
 */
@RestController
public class SubmitLink extends ExecuteListener {
    static Model model;

    @GetMapping("/dummy")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @GetMapping("/test")
    public Model test() throws MalformedURLException, JSONException {
        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        ELEXISRest elexisRest = new ELEXISRest(endpoint);
        return elexisRest.getEntryAsTurtle("dictionary","entry1s");
    }

    /**
     * @param messageBody
     * @return
     * @throws MalformedURLException
     * @throws JSONException
     * @throws JsonProcessingException
     */
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    public Model submitLinkRequest(@RequestBody MessageBody messageBody) throws MalformedURLException, JSONException, JsonProcessingException {
        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
//        URL endpoint = new URL(messageBody.getSource().getEndpoint());
        ELEXISRest elexisRest = new ELEXISRest(endpoint);
        model = elexisRest.getEntryAsTurtle("dictionary","entry1");

        List<Lemma[]> obj = new ArrayList<Lemma[]>();

        List<String> entries = messageBody.getSource().getEntries();
        for(int i = 0; i < entries.size() ; i++)
        {
            obj.add(i, elexisRest.getHeadWordLookup(messageBody.getSource().getId(),
                    entries.get(i).split("-")[0]));
        }
        return model;
    }

    @GetMapping("/status")
    public String getLinkingStatus()
    {

        return null;
    }
}