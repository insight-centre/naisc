package org.insightcentre.uld.naisc.main;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
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

    /**
     * Test of evaluate method, of class Evaluate.
     */
    @Test
    public void testEvaluate() {
        System.out.println("evaluate");
        AlignmentSet output = new AlignmentSet();
        output.add(new Alignment("file:id1", "file:id2", 0.0, Alignment.SKOS_EXACT_MATCH));
        output.add(new Alignment("file:id2", "file:id3", 0.56, Alignment.SKOS_EXACT_MATCH));
        output.add(new Alignment("file:id3", "file:id1", 1.0, Alignment.SKOS_EXACT_MATCH));
        output.add(new Alignment("file:id1", "file:id2", 0.3, "file:customProp"));
        Map<Property, Object2DoubleMap<Statement>> gold = new HashMap<>();
        Model m = ModelFactory.createDefaultModel();
        Property exactMatch = m.createProperty(Alignment.SKOS_EXACT_MATCH);
        Property redHerring = m.createProperty("file:redHerring");
        Object2DoubleMap exactMap = new Object2DoubleOpenHashMap();
        exactMap.put(m.createStatement(m.createResource("file:id1"), exactMatch, m.createResource("file:id2")), 0.5);
        exactMap.put(m.createStatement(m.createResource("file:id2"), exactMatch, m.createResource("file:id1")), 0.3);
        Object2DoubleMap herringMap = new Object2DoubleOpenHashMap();
        herringMap.put(m.createStatement(m.createResource("file:id1"), redHerring, m.createResource("file:id2")), 0.3);
        gold.put(exactMatch, exactMap);
        gold.put(redHerring, herringMap);
        
        Evaluate.EvaluationResults expResult = new Evaluate.EvaluationResults();
        expResult.tp = 1;
        expResult.fp = 3;
        expResult.fn = 2;
        Evaluate.EvaluationResults result = Evaluate.evaluate(output, gold);
        assertEquals(expResult.tp, result.tp);
        assertEquals(expResult.fp, result.fp);
        assertEquals(expResult.fn, result.fn);
    }


}