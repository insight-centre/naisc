package org.insightcentre.uld.naisc.feature;

import eu.monnetproject.lang.Language;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static org.apache.jena.sparql.vocabulary.TestManifest.result;

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
public class WordNetTest {

    public WordNetTest() {
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
     * Test of makeFeatureExtractor method, of class WordNet.
     */
    @Test
    public void testMakeFeatureExtractor() {
        System.out.println("makeFeatureExtractor");
        Set<String> tags = null;
        Map<String, Object> params = new HashMap<>();
        params.put("wordnetXmlFile", "src/test/resources/wnTest.xml");
        WordNet instance = new WordNet();
        TextFeature extractor = instance.makeFeatureExtractor(tags, params);
        Feature[] features = extractor.extractFeatures(new LensResult(Language.ENGLISH, Language.ENGLISH, "cat", "dog", null));
        assert(features.length == 8);
        for(int i = 0; i < 8; i++) {
            assert(Double.isFinite(features[i].value));
            assert(features[i].value > 0);
        }
    }

}