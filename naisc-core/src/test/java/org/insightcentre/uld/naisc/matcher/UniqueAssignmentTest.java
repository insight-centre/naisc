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
    private Resource r(String s) {
        return m.createResource(new AnonId(s));
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
        alignments.add(new Alignment(r("id1"), r("id1"), 0.5, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment(r("id1"), r("id2"), 0.9, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment(r("id2"), r("id2"), 0.7, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment(r("id3"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment(r("id2"), r("id3"), 0.0, Alignment.SKOS_EXACT_MATCH));
        AlignmentSet result = matcher.align(new AlignmentSet(alignments));
        assert(result.size() == 3);
        assert(result.contains(new Alignment(r("id1"), r("id1"), 0.5, Alignment.SKOS_EXACT_MATCH)));
        assert(result.contains(new Alignment(r("id2"), r("id2"), 0.7, Alignment.SKOS_EXACT_MATCH)));
        assert(result.contains(new Alignment(r("id3"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH)));
        
        params.put("threshold", 0.5);
        matcher = instance.makeMatcher(params);
        result = matcher.align(new AlignmentSet(alignments));
        assert(result.size() == 2);
        assert(result.contains(new Alignment(r("id1"), r("id1"), 0.5, Alignment.SKOS_EXACT_MATCH)));
        assert(result.contains(new Alignment(r("id2"), r("id2"), 0.7, Alignment.SKOS_EXACT_MATCH)));
        
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
        alignments.add(new Alignment(r("id1"), r("id1"), 0.5, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment(r("id1"), r("id2"), 0.9, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment(r("id2"), r("id2"), 0.7, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment(r("id3"), r("id1"), 0.1, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment(r("id3"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment(r("id2"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH));
        AlignmentSet initial = new AlignmentSet();
        initial.add(new Alignment(r("id1"), r("id2"), 1.0, Alignment.SKOS_EXACT_MATCH));
        AlignmentSet result = matcher.alignWith(new AlignmentSet(alignments), initial, NONE);
        assert(result.size() == 3);
        assert(result.contains(new Alignment(r("id1"), r("id2"), 1.0, Alignment.SKOS_EXACT_MATCH)));
        assert(result.contains(new Alignment(r("id2"), r("id3"), 0.1, Alignment.SKOS_EXACT_MATCH)));
        assert(result.contains(new Alignment(r("id3"), r("id1"), 0.1, Alignment.SKOS_EXACT_MATCH)));
        
    }
    
}