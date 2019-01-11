
package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import java.io.IOException;
import java.util.Map;
import org.insightcentre.uld.naisc.feature.BasicString.BasicStringImpl;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.PrettyGoodTokenizer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class BasicStringTest {

    public BasicStringTest() {
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
     * Test of id method, of class ClassifierFeatures.
     */
    @Test
    public void testId() {
        System.out.println("id");
        BasicStringImpl instance = new BasicStringImpl(false, null, null, null, null);
        String expResult = "basic-features";
        String result = instance.id();
        assertEquals(expResult, result);
    }

    /**
     * Test of getFeatureNames method, of class ClassifierFeatures.
     */
    @Test
    public void testGetFeatureNames() {
        System.out.println("getFeatureNames");
        BasicStringImpl instance = new BasicStringImpl(false, null, null, null, null);
        String[] expResult = new String[] {
            "lcs",
            "ngram-1",
            "ngram-2",
            "ngram-3",
            "ngram-4",
            "jaccard",
            "dice",
            "containment",
            "senLenRatio",
            "aveWordLenRatio",
            "negation",
            "number"
        };
        String[] result = instance.getFeatureNames();
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of extractFeatures method, of class ClassifierFeatures.
     */
    @Test
    public void testExtractFeatures() {
        System.out.println("extractFeatures");
        LangStringPair _sp = new LangStringPair(Language.ENGLISH, Language.ENGLISH, "foo", "bar");
        String entity1id = "x1";
        String entity2id = "x2";
        BasicStringImpl instance = new BasicStringImpl(false, null, null, null, null);
        double[] expResult = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0 };
        double[] result = instance.extractFeatures(_sp);
        assertArrayEquals(expResult, result, 0.0001);
    }

    /**
     * Test of longestCommonSubsequence method, of class ClassifierFeatures.
     */
    @Test
    public void testLongestCommonSubsequence() {
        System.out.println("longestCommonSubsequence");
        String[] s1 = new String[] { "a","b","d","a","b","c" };
        String[] s2 = new String[] { "a","b","c","d" };
        double expResult = 0.5;
        double result = BasicStringImpl.longestCommonSubsequence(s1, s2);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of ngramOverlap method, of class ClassifierFeatures.
     */
    @Test
    public void testNgramOverlap() {
        System.out.println("ngramOverlap");
        String[] s1 = new String[] { "a","b","d","a","b","c" };
        String[] s2 = new String[] { "a","b","c","d" };
        int n = 2;
        BasicStringImpl.NGramWeighting weighting = new BasicStringImpl.ConstantWeighting();
        double expResult = 0.6;
        double result = BasicStringImpl.ngramOverlap(s1, s2, n, weighting);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of jaccardDice method, of class ClassifierFeatures.
     */
    @Test
    public void testJaccardDice() {
        System.out.println("jaccardDice");
        String[] s1 = new String[] { "a","b","d","a","b","c" };
        String[] s2 = new String[] { "a","b","c" };
        BasicStringImpl.JaccardDice expResult = new BasicStringImpl.JaccardDice(0.75,6.0/7.0,1.0);
        BasicStringImpl.JaccardDice result = BasicStringImpl.jaccardDice(s1, s2);
        assertEquals(expResult, result);
    }

    /**
     * Test of symmetrizedRatio method, of class ClassifierFeatures.
     */
    @Test
    public void testSymmetrizedRatio() {
        System.out.println("symmetrizedRatio");
        double x = 0.2;
        double y = 0.8;
        double expResult = 0.75;
        double result = BasicStringImpl.symmetrizedRatio(x, y);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of sentenceLengthRatio method, of class ClassifierFeatures.
     */
    @Test
    public void testSentenceLengthRatio() {
        System.out.println("sentenceLengthRatio");
        String s1 = "a b c d a b ";
        String s2 = "a b c d ";
        double expResult = 1.0/3.0;
        double result = BasicStringImpl.sentenceLengthRatio(s1, s2);
        assertEquals(expResult, result, 0.00001);
    }

    /**
     * Test of aveWordLenRatio method, of class ClassifierFeatures.
     */
    @Test
    public void testAveWordLenRatio() {
        System.out.println("aveWordLenRatio");
        String[] s1 = new String[] { "a","b","d","a","b","c" };
        String[] s2 = new String[] { "a","b","c","d" };
        double expResult = 0.0;
        double result = BasicStringImpl.aveWordLenRatio(s1, s2);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of close method, of class ClassifierFeatures.
     */
    @Test
    public void testClose() throws Exception {
        System.out.println("close");
        BasicStringImpl instance = new BasicStringImpl(false, null, null, null, null);
        instance.close();
    }

    /**
     * Test of shareNegation method, of class ClassifierFeatures.
     */
    @Test
    public void testShareNegation() {
        System.out.println("shareNegation");
        Language lang = Language.ENGLISH;
        String[] s1 = PrettyGoodTokenizer.tokenize("This is a positive sentence");
        String[] s2 = PrettyGoodTokenizer.tokenize("This isn't not a negative sentence");
        boolean expResult = false;
        boolean result = BasicStringImpl.shareNegation(lang, s1, s2);
        assertEquals(expResult, result);
    }

    /**
     * Test of numberAgree method, of class ClassifierFeatures.
     */
    @Test
    public void testNumberAgree() {
        System.out.println("numberAgree");
        String[] s1 = PrettyGoodTokenizer.tokenize("There is 1.0 reason this may not work");
        String[] s2 = PrettyGoodTokenizer.tokenize("There is 1 reason this may not work");
        double expResult = 1.0;
        double result = BasicStringImpl.numberAgree(s1, s2);
        assertEquals(expResult, result,0.0);
    }

    @Test
    public void testLoadConfig() throws IOException {
        String config = "{\n" +
"    \"blocking\": {\n" +
"        \"name\": \"blocking.ApproximateStringMatching\",\n" +
"        \"maxMatches\": 5,\n" +
"        \"property\": \"http://www.w3.org/2000/01/rdf-schema#label\"\n" +
"    },\n" +
"    \"lenses\": [{\n" +
"        \"name\": \"lens.Label\"\n" +
"    }],\n" +
"    \"textFeatures\": [{\n" +
"        \"name\": \"feature.BasicString\",\n" +
"        \"labelChar\": true,\n" +
"        \"features\": [ \"jaccard\" ]\n" +
"    }],\n" +
"    \"scorers\": [{\n" +
"        \"name\": \"scorer.LibSVM\",\n" +
"        \"modelFile\": \"models/jaccard.libsvm\"\n" +
"    }],\n" +
"    \"matcher\": {\n" +
"        \"name\": \"matcher.UniqueAssignment\"\n" +
"    },\n" +
"    \"description\": \"Simple baseline method using string metrics and a SVM\"\n" +
"}";
        Configuration c = new ObjectMapper().readValue(config, Configuration.class);
        
        String s = "{\"params\": { \"foo\": [\"bar\",\"baz\"] } }";
        
        Foo f = new ObjectMapper().readValue(s, Foo.class);
        c.makeTextFeatures();
        
    }
    
    private static class Foo {
        public Map<String, Object> params;
    }
}