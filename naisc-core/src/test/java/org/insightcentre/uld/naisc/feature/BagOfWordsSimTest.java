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
public class BagOfWordsSimTest {

    public BagOfWordsSimTest() {
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
     * Test of makeFeatureExtractor method, of class BagOfWordsSim.
     */
    @Test
    public void testMakeFeatureExtractor() {
        System.out.println("makeFeatureExtractor");
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("method", "jaccardExponential");
            put("weighting", 0.5);
        }};
        BagOfWordsSim instance = new BagOfWordsSim();
        TextFeature extractor = instance.makeFeatureExtractor(Collections.EMPTY_SET,   params);
        assertArrayEquals(new String[] { "bow" }, extractor.getFeatureNames());
        assertArrayEquals(new double [] { (1.0-Math.exp(-0.5))/(1.0-Math.exp(-2.5)-Math.exp(-1)+Math.exp(-0.5)) },
                     toDbA(extractor.extractFeatures(new LensResult(Language.ABKHAZIAN, Language.ABKHAZIAN, "a b c d e", "a z", null))), 0.0001);

    }
    private double[] toDbA(Feature[] f) {
        double[] d = new double[f.length];
        for(int i = 0; i < f.length; i++) {
            d[i] = f[i].value;
        }
        return d;
    }

}