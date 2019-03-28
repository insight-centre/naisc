package org.insightcentre.uld.naisc.main;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.STDERR;
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
public class EvaluateTest {

    public EvaluateTest() {
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
    private Resource r(String s) {
        return m.createResource(s);
    }
    /**
     * Test of evaluate method, of class Evaluate.
     */
    @Test
    public void testEvaluate() {
        System.out.println("evaluate");
        AlignmentSet output = new AlignmentSet();
        output.add(new Alignment(r("file:id1"), r("file:id2"), 0.0, Alignment.SKOS_EXACT_MATCH));
        output.add(new Alignment(r("file:id2"), r("file:id3"), 0.56, Alignment.SKOS_EXACT_MATCH));
        output.add(new Alignment(r("file:id3"), r("file:id1"), 1.0, Alignment.SKOS_EXACT_MATCH));
        output.add(new Alignment(r("file:id1"), r("file:id2"), 0.3, "file:customProp"));
        AlignmentSet gold = new AlignmentSet();
        gold.add(new Alignment(r("file:id1"), r("file:id2"), 0.5, Alignment.SKOS_EXACT_MATCH));
        gold.add(new Alignment(r("file:id2"), r("file:id1"), 0.3, Alignment.SKOS_EXACT_MATCH));
        gold.add(new Alignment(r("file:id1"),r("file:id2"), 0.3, "file:redHerring"));
        
        Evaluate.EvaluationResults expResult = new Evaluate.EvaluationResults();
        expResult.tp = 1;
        expResult.fp = 3;
        expResult.fn = 2;
        Evaluate.EvaluationResults result = Evaluate.evaluate(output, gold, STDERR);
        assertEquals(expResult.tp, result.tp);
        assertEquals(expResult.fp, result.fp);
        assertEquals(expResult.fn, result.fn);
    }


}