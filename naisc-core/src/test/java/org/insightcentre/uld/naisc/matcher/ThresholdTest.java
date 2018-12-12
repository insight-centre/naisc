package org.insightcentre.uld.naisc.matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Matcher;
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
public class ThresholdTest {

    public ThresholdTest() {
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
     * Test of id method, of class Threshold.
     */
    @Test
    public void testId() {
        System.out.println("id");
        Threshold instance = new Threshold();
        String expResult = "threshold";
        String result = instance.id();
        assertEquals(expResult, result);
    }

    /**
     * Test of makeMatcher method, of class Threshold.
     */
    @Test
    public void testMakeMatcher() {        
        System.out.println("makeMatcher");
        Map<String, Object> params = new HashMap<>();
        params.put("threshold", 0.5);
        Threshold instance = new Threshold();
        Matcher matcher = instance.makeMatcher(params);
        List<Alignment> alignments = new ArrayList<>();
        alignments.add(new Alignment("id1", "id1", 0.5, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment("id1", "id2", 0.9, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment("id2", "id2", 0.7, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment("id3", "id3", 0.1, Alignment.SKOS_EXACT_MATCH));
        alignments.add(new Alignment("id2", "id3", 0.0, Alignment.SKOS_EXACT_MATCH));
        AlignmentSet result = matcher.align(new AlignmentSet(alignments));
        assert(result.size() == 3);
        assert(result.contains(new Alignment("id1", "id1", 0.5, Alignment.SKOS_EXACT_MATCH)));
        assert(result.contains(new Alignment("id2", "id2", 0.7, Alignment.SKOS_EXACT_MATCH)));
        assert(result.contains(new Alignment("id1", "id2", 0.9, Alignment.SKOS_EXACT_MATCH)));
    }

}