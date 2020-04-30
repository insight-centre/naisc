package org.insightcentre.uld.naisc.feature;

import eu.monnetproject.lang.Language;
import org.insightcentre.uld.naisc.Feature;
import org.insightcentre.uld.naisc.LensResult;
import org.insightcentre.uld.naisc.TextFeature;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.*;

public class MachineTranslationTest {

    @Test
    public void testMachineTranslation() {
        MachineTranslation mt = new MachineTranslation();
        Map<String, Object> params = new HashMap<>();
        params.put("methods", Arrays.asList("BLEU", "METEOR", "chrF"));

        TextFeature extractor = mt.makeFeatureExtractor(new HashSet<>(), params);
        Feature[] features = extractor.extractFeatures(new LensResult(Language.ENGLISH, Language.ENGLISH, "the quick brown fox jumped over the lazy dog",
                "the fast brown fox jumped over the sleepy dog", null));
        assertEquals(3, features.length);

    }
}