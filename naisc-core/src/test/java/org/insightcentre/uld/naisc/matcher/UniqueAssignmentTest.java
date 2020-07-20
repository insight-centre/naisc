package org.insightcentre.uld.naisc.matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.constraint.Bijective;
import org.insightcentre.uld.naisc.constraint.Constraint;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.NONE;
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
public class UniqueAssignmentTest {

    public UniqueAssignmentTest() {
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

    /**
     * Test of id method, of class UniqueAssignment.
     */
    @Test
    public void testId() {
        System.out.println("id");
        UniqueAssignment instance = new UniqueAssignment();
        String expResult = "unique";
        String result = instance.id();
        assertEquals(expResult, result);
    }


    private Model m = ModelFactory.createDefaultModel();
    private URIRes r(String s) {
        return new URIRes(s, "dataset");
    }
    /**
     * Test of makeMatcher method, of class UniqueAssignment.
     */
    @Test
    public void testMakeMatcher() {
        System.out.println("makeMatcher");
        Map<String, Object> params = new HashMap<>();
        UniqueAssignment instance = new UniqueAssignment();
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
        
        params.put("threshold", 0.5);
        matcher = instance.makeMatcher(params);
        result = matcher.align(new AlignmentSet(alignments));
        assert(result.size() == 2);
        assert(result.contains(new Alignment(r("id1"), r("id1"), 0.5, Alignment.SKOS_EXACT_MATCH, null)));
        assert(result.contains(new Alignment(r("id2"), r("id2"), 0.7, Alignment.SKOS_EXACT_MATCH, null)));
        
    }

    
    @Test
    public void testAlignWith() {
        System.out.println("makeMatcher");
        Map<String, Object> params = new HashMap<>();
        params.put("constraint", new HashMap<String,String>());
        ((HashMap<String,String>)params.get("constraint")).put("name", "constraint.Bijective");
        UniqueAssignment instance = new UniqueAssignment();
        Matcher matcher = instance.makeMatcher(params);
        List<Alignment> alignments = new ArrayList<>();
        alignments.add(new Alignment(r("id1"), r("id1"), 0.5, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id1"), r("id2"), 0.9, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id2"), r("id2"), 0.7, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id3"), r("id1"), 0.1, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id3"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH, null));
        alignments.add(new Alignment(r("id2"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH, null));
        AlignmentSet initial = new AlignmentSet();
        initial.add(new Alignment(r("id1"), r("id2"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        AlignmentSet result = matcher.alignWith(new AlignmentSet(alignments), initial, NONE);
        assert(result.size() == 3);
        assert(result.contains(new Alignment(r("id1"), r("id2"), 1.0, Alignment.SKOS_EXACT_MATCH, null)));
        assert(result.contains(new Alignment(r("id2"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH, null)));
        assert(result.contains(new Alignment(r("id3"), r("id1"), 0.1, Alignment.SKOS_EXACT_MATCH, null)));
        
    }
    
    private static AlignmentSet genAlignSet(int n, int seed) {
        AlignmentSet as = new AlignmentSet();
        Random r = new Random(seed);
        Model m = ModelFactory.createDefaultModel();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                    as.add(new Alignment(new URIRes("file:id" + i, "left"), new URIRes("file:id" + j, "right"), r.nextDouble()));
            }
        }
        return as;
    }
    
    
    @Test
    public void testHard() {
        UniqueAssignment instance = new UniqueAssignment();
        Matcher matcher = instance.makeMatcher(new HashMap<>());
        AlignmentSet matches = genAlignSet(10, 0);
        AlignmentSet result = matcher.alignWith(matches, new AlignmentSet(), NONE);
        assert(result.size() == 10);
        Constraint bijective = new Bijective().make(new HashMap<>());
        for(Alignment a : result) {
            bijective.add(a);
        }
        //assert(bijective.score > 6.744);
        
    }
}