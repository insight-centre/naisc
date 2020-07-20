package org.insightcentre.uld.naisc.feature;

import eu.monnetproject.lang.Language;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
public class WordEmbeddingsTest {

    public WordEmbeddingsTest() {
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
     * Test of makeFeatureExtractor method, of class WordEmbeddings.
     */
    @Test
    public void testMakeFeatureExtractor() {
        System.out.println("makeFeatureExtractor");
        Set<String> tags = Collections.EMPTY_SET;
        Map<String, Object> params = new HashMap<>();
        params.put("embeddingPath", "src/test/resources/glove.test");
        WordEmbeddings instance = new WordEmbeddings();
        TextFeature extractor = instance.makeFeatureExtractor(tags, params);
        Feature[] features = extractor.extractFeatures(new LensResult(Language.ENGLISH, Language.ENGLISH, "this is a test", "this is also a test", null));
        System.err.println(Arrays.toString(features));
                
    }
    private double[] toDbA(Feature[] f) {
        double[] d = new double[f.length];
        for(int i = 0; i < f.length; i++) {
            d[i] = f[i].value;
        }
        return d;
    }

}