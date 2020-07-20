package org.insightcentre.uld.naisc.elexis.Controller;

import lombok.extern.java.Log;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.insightcentre.uld.naisc.elexis.Helper.AsyncDictionaryLinkingHelper;
import org.insightcentre.uld.naisc.elexis.Model.*;
import org.insightcentre.uld.naisc.elexis.Model.MessageBody;
import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRest;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@Log
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
    public ResponseEntity<DefaultResponse> defaultGet() throws MalformedURLException, JSONException {
//        URL endpoint = new URL("http://localhost:8000/");
        URL endpoint = new URL("http://server1.nlp.insight-centre.org:9019/");
        ELEXISRest elexisRest = new ELEXISRest(endpoint);

        log.info("[ Initiating default request ]");
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setDictionaries((ArrayList<String>) elexisRest.getDictionaries());
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
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
    public ResponseEntity<SubmitResponse> submitLinkRequest(@RequestBody MessageBody messageBody)
            throws IOException, TransformerException {
        String requestId = UUID.randomUUID().toString();
        AsyncDictionaryLinkingHelper asyncHelper = context.getBean(AsyncDictionaryLinkingHelper.class);
        inMemoryRequestStore.put(requestId, asyncHelper);

        // Using an object of AsyncDictionaryLinkingHelper, RequestStatusListener to initiate the request in background
        log.info("[ Loading details for submit request for "+requestId+" ]");
        asyncHelper.setRequestId(requestId);
        asyncHelper.getConfigurationDetails(messageBody);
        asyncHelper.getDictionaryDetails(messageBody);

        // Calling the execute method with the generated files
        log.info("[ Initiating submit request for "+requestId+" ]");
        asyncHelper.asyncExecute();

        // Returning the requestId for the request
        SubmitResponse submitResponse = new SubmitResponse();
        submitResponse.setRequestId(requestId);
        return new ResponseEntity<>(submitResponse, HttpStatus.OK);
    }

    /**
     * API call to retrieve the status of linking
     *
     * @return
     */
    @PostMapping(value = "/status")
    public ResponseEntity<StatusResponse> getLinkStatus(@RequestBody GenericMessageBody message) {

        AsyncDictionaryLinkingHelper asyncHelper = inMemoryRequestStore.get(message.getRequestId());
        String status = asyncHelper.getRequestStatusListener().getStage().name();
        StatusResponse statusResponse = new StatusResponse();

        log.info("[ Checking the request status for "+asyncHelper.getRequestId()+" ]");
        if (status.equals("COMPLETED") || status.equals("FAILED")) {
            statusResponse.setState("COMPLETED");
            statusResponse.setMessage("Results are ready");
        } else {
            statusResponse.setState("PROCESSING");
            statusResponse.setMessage("Still working away");
        }
        return new ResponseEntity<>(statusResponse, HttpStatus.OK);
    }

    /**
     * API call to return the linking response
     *
     * @return Linking Results
     */
    @PostMapping("/result")
    public ResponseEntity<Object> getResults(@RequestBody GenericMessageBody message) {
        AsyncDictionaryLinkingHelper asyncHelper = inMemoryRequestStore.get(message.getRequestId());
        String status = asyncHelper.getRequestStatusListener().getStage().name();

        if(status.equals("FAILED")) {
            log.severe("[ Request status FAILED for "+asyncHelper.getRequestId()+" ]");
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (status.equals("COMPLETED")) {
            log.info("[ Fetching results for the request "+asyncHelper.getRequestId()+" ]");
            ArrayList<Result> results = asyncHelper.generateResults();
            return new ResponseEntity(results, HttpStatus.OK);
        } else {
            log.info("[ Request still in progress for "+asyncHelper.getRequestId()+" ]");
            return new ResponseEntity(HttpStatus.ACCEPTED);
        }
    }
}