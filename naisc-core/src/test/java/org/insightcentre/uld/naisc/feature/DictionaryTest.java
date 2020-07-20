package org.insightcentre.uld.naisc.feature;

import eu.monnetproject.lang.Language;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
public class DictionaryTest {

    public DictionaryTest() {
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
     * Test of makeFeatureExtractor method, of class Dictionary.
     */
    @Test
    public void testMakeFeatureExtractor() {
        System.out.println("makeFeatureExtractor");
        Set<String> tags = Collections.EMPTY_SET;
        Map<String, Object> params = new HashMap<>();
        params.put("dict","src/test/resources/example.dict");
        Dictionary instance = new Dictionary();
        TextFeature extractor = instance.makeFeatureExtractor(tags, params);
        assertEquals(extractor.extractFeatures(new LensResult(Language.ENGLISH, Language.ENGLISH, "cat", "feline", null))[0].value,1.0,0.0);
        assertEquals(extractor.extractFeatures(new LensResult(Language.ENGLISH, Language.ENGLISH, "cat", "canine", null))[0].value,0.0,0.0);
        
    }

}