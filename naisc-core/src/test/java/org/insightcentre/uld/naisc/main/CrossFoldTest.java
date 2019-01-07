package org.insightcentre.uld.naisc.main;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.insightcentre.uld.naisc.Alignment;
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
public class CrossFoldTest {

    public CrossFoldTest() {
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
     * Test of splitDataset method, of class CrossFold.
     */
    @Test
    public void testSplitDataset() {
        System.out.println("splitDataset");
        Model model = ModelFactory.createDefaultModel();
        Property p = model.createProperty(Alignment.SKOS_EXACT_MATCH);
        Object2DoubleMap<Statement> s = new Object2DoubleOpenHashMap<>();
        s.put(model.createStatement(model.createResource("file:left#e1"), p, model.createResource("file:right#e1")), 1.0);
        s.put(model.createStatement(model.createResource("file:left#e2"), p, model.createResource("file:right#e2")), 1.0);
        s.put(model.createStatement(model.createResource("file:left#e3"), p, model.createResource("file:right#e3")), 1.0);
        s.put(model.createStatement(model.createResource("file:left#e4"), p, model.createResource("file:right#e4")), 1.0);
        Map<Property, Object2DoubleMap<Statement>> alignments =  new HashMap<>();
        alignments.put(p, s);
        int folds = 4;
        CrossFold instance = new CrossFold();
        CrossFold.Result result = instance.splitDataset(alignments, folds);
        assertEquals(folds,result.leftSplit.size());
        assertEquals(folds,result.rightSplit.size());
        for(int i = 0 ; i < folds; i++) {
            assertEquals(1, result.leftSplit.get(i).size());
            assertEquals(1, result.rightSplit.get(i).size());
        }
    }

}