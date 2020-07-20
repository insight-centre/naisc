package org.insightcentre.uld.naisc.feature;

import eu.monnetproject.lang.Language;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.insightcentre.uld.naisc.Feature;
import org.insightcentre.uld.naisc.LensResult;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.insightcentre.uld.naisc.TextFeature;

/**
 *
 * @author John McCrae
 */
public class KeyWordsTest {

    public KeyWordsTest() {
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
     * Test of makeFeatureExtractor method, of class KeyWords.
     */
    @Test
    public void testMakeFeatureExtractor() {
        System.out.println("makeFeatureExtractor");
        Map<String, Object> params = new HashMap<>();
        params.put("keywordsFile", "src/test/resources/keywords.txt");
        KeyWords instance = new KeyWords();
        TextFeature extractor = instance.makeFeatureExtractor(Collections.EMPTY_SET, params);
        Feature[] result = extractor.extractFeatures(new LensResult(Language.ENGLISH, Language.ENGLISH,
                "a cat such as an american shorthair", "american cats include the maine coon and the american shorthair",
                null));
        double[] expResult = new double[] { 2.0/4.0, 1.0/3.0 };
        assertArrayEquals(expResult, toDbA(result), 0.0000001);
    }
    private double[] toDbA(Feature[] f) {
        double[] d = new double[f.length];
        for(int i = 0; i < f.length; i++) {
            d[i] = f[i].value;
        }
        return d;
    }

}