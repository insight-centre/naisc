package org.insightcentre.uld.naisc.elexis.Controller;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.insightcentre.uld.naisc.elexis.Helper.AsyncDictionaryLinkingHelper;
import org.insightcentre.uld.naisc.elexis.Model.*;
import org.insightcentre.uld.naisc.elexis.Model.MessageBody;
import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRest;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Implementation of ELEXIS Linking APIs to return the linking between two dictionaries
 * defined at: https://elexis-eu.github.io/elexis-rest/linking.html#
 *
 * @author Suruchi Gupta
 */
@RestController
public class SubmitLink {
    @Autowired
    ApplicationContext context;
    private PassiveExpiringMap<String, AsyncDictionaryLinkingHelper> inMemoryRequestStore;

    @PostConstruct
    public void postConstruct() {
        inMemoryRequestStore = new PassiveExpiringMap<>();
    }

    /**
     * Default call to the ELEXIS Linking API
     *
     * @return List of available dictionaries
     * @throws MalformedURLException
     * @throws JSONException
     */
    @GetMapping("/")
    public DefaultResponse test() throws MalformedURLException, JSONException {
        URL endpoint = new URL("http://localhost:8000/");
//        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        ELEXISRest elexisRest = new ELEXISRest(endpoint);

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setDictionaries((ArrayList<String>) elexisRest.getDictionaries());
        return defaultResponse;
    }

    /**
     * API call to link the sense of two dictionaries
     *
     * @param messageBody
     * @return requestID for the request
     * @throws JSONException
     * @throws IOException
     */
    @PostMapping(value = "/submit")
    public SubmitResponse submitLinkRequest(@RequestBody MessageBody messageBody)
            throws IOException, TransformerException {
        String requestId = UUID.randomUUID().toString();
        AsyncDictionaryLinkingHelper asyncHelper = context.getBean(AsyncDictionaryLinkingHelper.class);
        inMemoryRequestStore.put(requestId, asyncHelper);

        // Using an object of AsyncDictionaryLinkingHelper, RequestStatusListener to initiate the request in background
        asyncHelper.setRequestId(requestId);
        asyncHelper.getConfigurationDetails(messageBody);
        asyncHelper.getDictionaryDetails(messageBody);

        // Calling the execute method with the generated files
        asyncHelper.asyncExecute();

        // Returning the requestId for the request
        SubmitResponse submitResponse = new SubmitResponse();
        submitResponse.setRequestId(requestId);
        return submitResponse;
    }

    /**
     * API call to retrieve the status of linking
     *
     * @return
     */
    @PostMapping(value = "/status")
    public StatusResponse getLinkStatus(@RequestBody GenericMessageBody message) {

        String status = inMemoryRequestStore.get(message.getRequestId()).getRequestStatusListener().getStage().name();
        StatusResponse statusResponse = new StatusResponse();

        if (status.equals("COMPLETED") || status.equals("FAILED")) {
            statusResponse.setState("COMPLETED");
            statusResponse.setMessage("Results are ready");
        } else {
            statusResponse.setState("PROCESSING");
            statusResponse.setMessage("Still working away");
        }
        return statusResponse;
    }

    /**
     * API call to return the linking response
     *
     * @return Linking Results
     */
    @PostMapping("/result")
    public ArrayList<Result> getResults(@RequestBody GenericMessageBody message) {
        AsyncDictionaryLinkingHelper asyncHelper = inMemoryRequestStore.get(message.getRequestId());
        String status = asyncHelper.getRequestStatusListener().getStage().name();

        ArrayList<Result> results = new ArrayList<>();
        if (status.equals("COMPLETED") || status.equals("MATCHING")) {
            results = asyncHelper.generateResults();
        }
        return results;
    }
}