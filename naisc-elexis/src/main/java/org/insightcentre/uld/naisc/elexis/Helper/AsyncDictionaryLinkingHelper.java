package org.insightcentre.uld.naisc.elexis.Helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.message.Message;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.elexis.Model.Linking;
import org.insightcentre.uld.naisc.elexis.Model.MessageBody;
import org.insightcentre.uld.naisc.elexis.Model.Result;
import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRest;
import org.insightcentre.uld.naisc.elexis.RestService.Lemma;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
import org.insightcentre.uld.naisc.scorer.ModelNotTrainedException;
import org.insightcentre.uld.naisc.util.None;

import org.insightcentre.uld.naisc.util.Pair;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * Implementation of Helper services for linking request
 *
 * @author Suruchi Gupta
 */
@Log
@Service
@Scope(value = "prototype")
public class AsyncDictionaryLinkingHelper {
    private final String requestId;
    //Configuration config = null;
    private final RequestStatusListener requestStatusListener;

    private Model leftModel = null, rightModel = null;
    private URL sourceEndpoint = null, targetEndpoint = null;
    private String sourceId = null, targetId = null;
    private AlignmentSet alignments;
    private ArrayList<Result> results;

    public AsyncDictionaryLinkingHelper(String requestId) {
        requestStatusListener = new RequestStatusListener();
        results = new ArrayList<>();
        this.requestId = requestId;
    }

    public RequestStatusListener getRequestStatusListener() {
        return requestStatusListener;
    }

    public String getRequestId() {
        return requestId;
    }

    /**
     * The Method reads the configuration sent in the messageBody or sets the default configuration
     *
     * @param messageBody
     * @throws IOException
     */
    public Configuration getConfigurationDetails(MessageBody messageBody) throws IOException {
        if (messageBody.getConfiguration() == null) {
            Configuration config = new Configuration(
                    new Configuration.BlockingStrategyConfiguration("blocking.OntoLex", Collections.EMPTY_MAP),
                    Arrays.asList(new Configuration.LensConfiguration("lens.OntoLex", Collections.EMPTY_MAP)),
                    Collections.EMPTY_LIST,
                    Arrays.asList(new Configuration.TextFeatureConfiguration("feature.BasicString", Collections.EMPTY_MAP, Collections.EMPTY_SET)),
                    Arrays.asList(new Configuration.ScorerConfiguration("scorer.Average", Collections.EMPTY_MAP, null)),
                    new Configuration.MatcherConfiguration("matcher.Greedy", Collections.singletonMap("constraint", Collections.singletonMap("name", "constraint.Bijective"))),
                    "Default ELEXIS matching profile",
                    Configuration.RescalerMethod.NoScaling);
            return config;
        } else {
            // Setting the configurations sent in the MessageBody
            log.info("[ Loading config from messageBody for request: "+requestId+" ]");
            return messageBody.getConfiguration();
        }
    }

    /**
     * Method to return all the valid entries for linking
     *
     * @param entries
     * @param elexisRest
     * @param id
     * @return
     * @throws IOException
     * @throws TransformerException
     */
    private Model processDictionary(ArrayList<String> entries, ELEXISRest elexisRest,
                                    String id, ArrayList<String> lemmaForms) throws IOException, TransformerException {

        // Getting all the lemmas from the dictionary and creating a map
        Lemma[] lemmas = elexisRest.getAllLemmas(id);
        HashMap<String, Lemma> map = new HashMap<String, Lemma>();
        for (Lemma l : lemmas) {
            map.put(l.getId(), l);
            if(lemmaForms != null)
                lemmaForms.add(l.getLemma());
        }

        // Creating a model and retrieving the relevant entries
        Model model = ModelFactory.createDefaultModel();
        if (null != entries && entries.size() != 0) {
            log.info("[ Loading entries from messageBody for request: "+requestId+" ]");
            for (String entry : entries) {
                model.add(getEntry(map, elexisRest, id, entry));
            }
        } else {
            log.info("[ Loading all entries for request: "+requestId+" ]");
            for (Lemma l : lemmas) {
                model.add(getEntry(map, elexisRest, id, l.getId()));
            }
        }
        return model;
    }

    /**
     * Method to return the lemma as model based on the available format
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
        if (formats.contains("ontolex")) {
            return elexisRest.getEntryAsTurtle(id, entry);
        } else if (formats.contains("tei")) {
            return elexisRest.getEntryAsTEI(id, entry);
        } else {
            return elexisRest.getEntryAsJSON(id, entry);
        }
    }

    /**
     * The method uses the parameters sent in the messageBody to generate the linking request
     *
     * @param messageBody
     * @throws IOException
     * @throws TransformerException
     */
    private void prepareAsyncRun(MessageBody messageBody) throws IOException, TransformerException {
        // Processing source object from MessageBody
        sourceEndpoint = new URL(messageBody.getSource().getEndpoint());
        sourceId = messageBody.getSource().getId();
        ELEXISRest elexisRest = ELEXISRest.factory.make(sourceEndpoint);

        // Getting the left dictionary id and the entries from messageBody
        log.info("[ Loading Source details for request: "+requestId+" ]");
        String sourceId = messageBody.getSource().getId();
        ArrayList<String> sourceEntries = messageBody.getSource().getEntries();
        ArrayList<String> lemmaForms = new ArrayList<>();
        leftModel = processDictionary(sourceEntries, elexisRest, sourceId, lemmaForms);


        // Processing target object from MessageBody
        targetEndpoint = new URL(messageBody.getTarget().getEndpoint());
        targetId = messageBody.getTarget().getId();
        elexisRest = ELEXISRest.factory.make(targetEndpoint);

        // Getting the right dictionary id and the entries from messageBody
        log.info("[ Loading Target details for request: "+requestId+" ]");
        String targetId = messageBody.getTarget().getId();
        ArrayList<String> targetEntries = messageBody.getTarget().getEntries();
        if(targetEntries == null) {
            targetEntries = new ArrayList<>();
            for(String lemma : lemmaForms) {
                Lemma[] lemmas = elexisRest.getHeadWordLookup(targetId, lemma);
                if(lemmas != null) {
                    for(Lemma l : lemmas) {
                        targetEntries.add(l.getId());
                    }
                }
            }
        }
        rightModel = processDictionary(targetEntries, elexisRest, targetId, null);

    }

    /**
     * Method to run the linking for the dictionaries in background
     */
    @Async
    public void asyncExecute(MessageBody body, Configuration config) throws IllegalStateException, ModelNotTrainedException, IOException, TransformerException {
        prepareAsyncRun(body);
        alignments = org.insightcentre.uld.naisc.main.Main.execute(requestId,
                new DefaultDatasetLoader.ModelDataset(leftModel, sourceId, sourceEndpoint),
                new DefaultDatasetLoader.ModelDataset(rightModel, targetId, targetEndpoint),
                config, new None<>(), requestStatusListener, null, null, new DefaultDatasetLoader());
        requestStatusListener.updateStatus(NaiscListener.Stage.COMPLETED, "Linking Process Complete");
    }

    /**
     * The method returns the response for the linking request
     *
     * @return ArrayList of Result
     */
    public ArrayList<Result> generateResults() {
        results = new ArrayList<>();

        Property left = leftModel.createProperty("http://www.w3.org/ns/lemon/ontolex#sense");
        Property right = rightModel.createProperty("http://www.w3.org/ns/lemon/ontolex#sense");

        for (Alignment a : alignments.getAlignments()) {
            Result result = new Result();
            ArrayList<Linking> links = new ArrayList<>();
            Linking link = new Linking();

            String lefturi = a.entity1.uri;
            link.setSourceSense(lefturi);

            // Getting the Source entry based on the link returned
            ResIterator resIterator = leftModel.listResourcesWithProperty(left);
            while (resIterator.hasNext()) {
                Resource r = resIterator.nextResource();
                for (Statement s : r.listProperties(left).toList()) {
                    if (s.getObject().toString().equals(lefturi))
                        result.setSourceEntry(r.getLocalName());
                }
            }

            String righturi = a.entity2.uri;
            link.setTargetSense(righturi);

            // Getting the Target entry based on the link returned
            resIterator = rightModel.listResourcesWithProperty(right);
            while (resIterator.hasNext()) {
                Resource r = resIterator.nextResource();
                for (Statement s : r.listProperties(right).toList()) {
                    if (s.getObject().toString().equals(righturi))
                        result.setTargetEntry(r.getLocalName());
                }
            }

            link.setScore(a.probability);
            link.setType(mapResult(a.property));

            links.add(link);
            result.setLinking(links);
            results.add(result);
        }
        return results;
    }

    /**
     * Method to map the link type
     * @param result
     * @return mapped link type
     */
    private String mapResult(String result) {
        if(result.equals(Alignment.SKOS_EXACT_MATCH)) {
            return "exact";
        } else if(result.equals(SKOS.narrowMatch.toString())) {
            return "narrower";
        } else if(result.equals(SKOS.broadMatch.toString())) {
            return "narrower";
        } else if(result.equals(SKOS.relatedMatch.toString())) {
            return "narrower";
        } else {
            System.err.println("Unrecognized property: " + result);
            return "none";
        }
    }

}
