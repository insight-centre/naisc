package org.insightcentre.uld.naisc.matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.URIRes;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author John McCrae
 */
public class MonteCarloTreeSearchTest {

    public MonteCarloTreeSearchTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    private Model m = ModelFactory.createDefaultModel();
    private URIRes r(String s) {
        return new URIRes(s, "dataset");
    }

    /**
     * Test of id method, of class MonteCarloTreeSearch.
     */
    @Test
    public void testId() {
        System.out.println("id");
        MonteCarloTreeSearch instance = new MonteCarloTreeSearch();
        String expResult = "monte-carlo";
        String result = instance.id();
        assertEquals(expResult, result);
    }

    /**
     * Test of makeMatcher method, of class MonteCarloTreeSearch.
     */
    @Test
    public void testMakeMatcher() {
                System.out.println("makeMatcher");
        Map<String, Object> params = new HashMap<>();
        params.put("constraint", new HashMap<String,String>());
        ((HashMap<String,String>)params.get("constraint")).put("name", "constraint.ThresholdConstraint");
        params.put("maxIterations", 32);
        MonteCarloTreeSearch instance = new MonteCarloTreeSearch();
        Matcher matcher = instance.makeMatcher(params);
        List<Alignment> alignments = new ArrayList<>();
        alignments.add(new Alignment(r("id1"), r("id1"), 0.5, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id1"), r("id2"), 0.9, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id2"), r("id2"), 0.7, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id3"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id2"), r("id3"), 0.0, Alignment.SKOS_EXACT_MATCH, null));
        AlignmentSet result = matcher.align(new AlignmentSet(alignments));
        assert(result.size() == 5);
        //assert(result.contains(new Alignment("id1", "id1", 0.5, Alignment.SKOS_EXACT_MATCH)));
        //assert(result.contains(new Alignment("id2", "id2", 0.7, Alignment.SKOS_EXACT_MATCH)));
        //assert(result.contains(new Alignment("id1", "id2", 0.9, Alignment.SKOS_EXACT_MATCH)));

    }
    
    @Test
    public void testMakeMatcher2() {
        //for(int i = 0; i < 100; i++) {
        System.out.println("makeMatcher");
        Map<String, Object> params = new HashMap<>();
        params.put("constraint", new HashMap<String,String>());
        ((HashMap<String,String>)params.get("constraint")).put("name", "constraint.Bijective");
        params.put("maxIterations", 100);
        MonteCarloTreeSearch instance = new MonteCarloTreeSearch();
        Matcher matcher = instance.makeMatcher(params);
        List<Alignment> alignments = new ArrayList<>();
        alignments.add(new Alignment(r("id1"), r("id1"), 0.5, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id1"), r("id2"), 0.9, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id2"), r("id2"), 0.7, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id3"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id2"), r("id3"), 0.0, Alignment.SKOS_EXACT_MATCH, null));
        AlignmentSet result = matcher.align(new AlignmentSet(alignments));
        assert(result.size() == 3);
        assert(result.contains(new Alignment(r("id1"), r("id1"), 0.5, Alignment.SKOS_EXACT_MATCH, null)));
        assert(result.contains(new Alignment(r("id2"), r("id2"), 0.7, Alignment.SKOS_EXACT_MATCH, null)));
        assert(result.contains(new Alignment(r("id3"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH, null)));
        //}
    }

}