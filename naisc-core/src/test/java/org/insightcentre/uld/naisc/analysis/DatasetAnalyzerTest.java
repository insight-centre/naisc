package org.insightcentre.uld.naisc.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.lens.Label;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
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
public class DatasetAnalyzerTest {

    public DatasetAnalyzerTest() {
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
     * Test of overlapSize method, of class DatasetAnalyzer.
     */
    @Test
    public void testOverlapSize() {
        System.out.println("overlapSize");
        Set<String> left = new HashSet<>();
        left.add("a");
        left.add("b");
        left.add("c");
        List<String> right = new ArrayList<>();
        right.add("c");
        right.add("d");
        right.add("e");
        DatasetAnalyzer instance = new DatasetAnalyzer();
        int expResult = 1;
        int result = instance.overlapSize(left, right);
        assertEquals(expResult, result);
    }

    /**
     * Test of analyseMatch method, of class DatasetAnalyzer.
     */
    @Test
    public void testAnalyseMatch() {
        System.out.println("analyseMatch");
        Map<String, List<String>> left = new HashMap<>();
        left.put("p1", Arrays.asList("a", "b", "c"));
        left.put("p2", Arrays.asList("c", "d", "e"));
        Map<String, List<String>> right = new HashMap<>();
        right.put("p3", Arrays.asList("a", "b"));
        right.put("p2", Arrays.asList("c", "d", "d", "e"));
        DatasetAnalyzer instance = new DatasetAnalyzer();
        List<MatchResult> expResult = new ArrayList<>();
        expResult.add(new MatchResult("p1", "p2", 3, 4, 1));
        expResult.add(new MatchResult("p1", "p3", 3, 2, 2));
        expResult.add(new MatchResult("p2", "p2", 3, 4, 4));
        expResult.add(new MatchResult("p2", "p3", 3, 2, 0));
        List<MatchResult> result = instance.analyseMatch(left, right);
        assertEquals(expResult, result);
    }

    /**
     * Test of analyseModel method, of class DatasetAnalyzer.
     */
    @Test
    public void testAnalyseModel() {
        System.out.println("analyseModel");
        Model leftModel = ModelFactory.createDefaultModel();
        final Resource res = leftModel.createResource("http://www.example.com/foo");
        final Resource res2 = leftModel.createResource("http://www.example.com/foo2");
        final Resource res3 = leftModel.createResource("http://www.example.com/foo3");
        final Resource res4 = leftModel.createResource("http://www.example.com/foo4");
        final Resource res5 = leftModel.createResource("http://www.example.com/foo5");

        leftModel.add(res,
                leftModel.createProperty(Label.RDFS_LABEL),
                leftModel.createLiteral("english", "en"));

        leftModel.add(res2,
                leftModel.createProperty(Label.RDFS_LABEL),
                leftModel.createLiteral("deutsch", "de"));

        leftModel.add(res3,
                leftModel.createProperty(Label.RDFS_LABEL),
                leftModel.createLiteral("???"));

        leftModel.add(res4,
                leftModel.createProperty(Label.RDFS_LABEL),
                leftModel.createLiteral("more english", "en"));

        leftModel.add(res5,
                leftModel.createProperty(Label.SKOS_PREFLABEL),
                leftModel.createLiteral("???"));

        Model rightModel = ModelFactory.createDefaultModel();
        
        rightModel.add(res,
                rightModel.createProperty(Label.RDFS_LABEL),
                rightModel.createLiteral("english", "en"));

        rightModel.add(res2,
                rightModel.createProperty(Label.RDFS_LABEL),
                rightModel.createLiteral("deutsch", "de"));

        rightModel.add(res3,
                rightModel.createProperty(Label.RDFS_LABEL),
                rightModel.createLiteral("???"));

        rightModel.add(res4,
                rightModel.createProperty(Label.RDFS_LABEL),
                rightModel.createLiteral("more english", "en"));

        rightModel.add(res5,
                rightModel.createProperty(Label.SKOS_PREFLABEL),
                rightModel.createLiteral("???"));
        DatasetAnalyzer instance = new DatasetAnalyzer();
        Analysis result = instance.analyseModel(new DefaultDatasetLoader.ModelDataset(leftModel, "left"), new DefaultDatasetLoader.ModelDataset(rightModel, "right"));
    }

    /**
     * Test of isNaturalLangLike method, of class DatasetAnalyzer.
     */
    @Test
    public void testIsNaturalLangLike() {
        System.out.println("isNaturalLangLike");
        String s = "this is a test";
        DatasetAnalyzer instance = new DatasetAnalyzer();
        boolean expResult = true;
        boolean result = instance.isNaturalLangLike(s);
        assertEquals(expResult, result);
        assertEquals(false, instance.isNaturalLangLike("MA12345"));
    }

    /**
     * Test of diversity method, of class DatasetAnalyzer.
     */
    @Test
    public void testDiversity() {
        System.out.println("diversity");
        List<String> strings = new ArrayList<>();
        strings.add("a");
        strings.add("a");
        strings.add("b");
        strings.add("b");
        strings.add("b");
        strings.add("c");

        DatasetAnalyzer instance = new DatasetAnalyzer();
        double expResult = 0.972;
        double result = instance.diversity(strings);
        assertEquals(expResult, result, 0.001);
    }

    /**
     * Test of uniqueness method, of class DatasetAnalyzer.
     */
    @Test
    public void testUniqueness() {
        System.out.println("uniqueness");
        List<String> strings = new ArrayList<>();
        strings.add("a");
        strings.add("a");
        strings.add("b");
        strings.add("b");
        strings.add("b");
        strings.add("c");
        DatasetAnalyzer instance = new DatasetAnalyzer();
        double expResult = 0.5;
        double result = instance.uniqueness(strings);
        assertEquals(expResult, result, 0.001);
    }

}
