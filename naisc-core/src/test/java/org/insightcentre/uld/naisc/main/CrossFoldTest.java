package org.insightcentre.uld.naisc.main;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.insightcentre.uld.naisc.Alignment;
import static org.insightcentre.uld.naisc.Alignment.SKOS_EXACT_MATCH;
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
        AlignmentSet alignments = new AlignmentSet();
        alignments.add(new Alignment("file:left#e1", "file:right#e1", 1.0, SKOS_EXACT_MATCH));
        alignments.add(new Alignment("file:left#e2", "file:right#e2", 1.0, SKOS_EXACT_MATCH));
        alignments.add(new Alignment("file:left#e3", "file:right#e3", 1.0, SKOS_EXACT_MATCH));
        alignments.add(new Alignment("file:left#e4", "file:right#e4", 1.0, SKOS_EXACT_MATCH));
        int folds = 4;
        CrossFold.Folds result = CrossFold.splitDataset(alignments, folds);
        assertEquals(folds,result.leftSplit.size());
        assertEquals(folds,result.rightSplit.size());
        for(int i = 0 ; i < folds; i++) {
            assertEquals(1, result.leftSplit.get(i).size());
            assertEquals(1, result.rightSplit.get(i).size());
        }
    }
    
    @Test
    public void testSplitDatasetRandom() {
        Model model = ModelFactory.createDefaultModel();
        Property p = model.createProperty(Alignment.SKOS_EXACT_MATCH);
        AlignmentSet alignments = new AlignmentSet();
        Random rand = new Random();
        for(int i = 0; i < 1000; i++) {
            int l = rand.nextInt(100);
            int r = rand.nextInt(100);
            alignments.add(new Alignment("file:left#e" + l, "file:right#e" + r, 1.0, SKOS_EXACT_MATCH));
            
        }
        int folds = 10;
        CrossFold.Folds result = CrossFold.splitDataset(alignments, folds);
        assertEquals(folds,result.leftSplit.size());
        assertEquals(folds,result.rightSplit.size());
        for(int i = 0 ; i < folds; i++) {
            System.err.print(result.leftSplit.get(i).size() + "/" + result.rightSplit.get(i).size() + " ");
        }
        System.err.println();
        for(int i = 0 ; i < folds; i++) {
            assert(5 <= result.leftSplit.get(i).size());
            assert(15 >= result.leftSplit.get(i).size());
            assert(5 <= result.rightSplit.get(i).size());
            assert(15 >= result.rightSplit.get(i).size());
        }
    }
    
    public void testResult() {
        System.out.println("testResult");
        Model model = ModelFactory.createDefaultModel();
        AlignmentSet alignments = new AlignmentSet();
        alignments.add(new Alignment("file:left#e1", "file:right#e1", 1.0, SKOS_EXACT_MATCH));
        alignments.add(new Alignment("file:left#e2", "file:right#e2", 1.0, SKOS_EXACT_MATCH));
        alignments.add(new Alignment("file:left#e2", "file:right#e3", 1.0, SKOS_EXACT_MATCH));
        alignments.add(new Alignment("file:left#e3", "file:right#e3", 1.0, SKOS_EXACT_MATCH));
        alignments.add(new Alignment("file:left#e4", "file:right#e4", 1.0, SKOS_EXACT_MATCH));
        List<Set<String>> leftSplit = new ArrayList<>();
        leftSplit.add(new HashSet<>());
        leftSplit.get(0).add("file:left#e1");
        leftSplit.get(0).add("file:left#e2");
        leftSplit.add(new HashSet<>());
        leftSplit.get(0).add("file:left#e3");
        leftSplit.get(0).add("file:left#e4");
        List<Set<String>> rightSplit = new ArrayList<>();
        rightSplit.add(new HashSet<>());
        rightSplit.get(0).add("file:right#e1");
        rightSplit.get(0).add("file:right#e2");
        rightSplit.add(new HashSet<>());
        rightSplit.get(0).add("file:right#e3");
        rightSplit.get(0).add("file:right#e4");
        CrossFold.Folds result = new CrossFold.Folds(leftSplit, rightSplit);
        AlignmentSet fold1 = result.train(alignments, 0);
        AlignmentSet fold2 = result.test(alignments, 1);
        assertEquals(2, fold1.size());
        assertEquals(2, fold2.size());
        assertEquals(fold1, result.train(alignments, 1));
        assertEquals(fold2, result.test(alignments, 2));
        
    }

}