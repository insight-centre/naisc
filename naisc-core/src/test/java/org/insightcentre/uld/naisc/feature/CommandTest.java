package org.insightcentre.uld.naisc.feature;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.insightcentre.uld.naisc.Feature;
import org.insightcentre.uld.naisc.TextFeature;
import org.insightcentre.uld.naisc.util.ExternalCommandException;
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
public class CommandTest {

    public CommandTest() {
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
     * Test of makeFeatureExtractor method, of class Command.
     */
    @Test
    public void testMakeFeatureExtractor() {
        if(System.getProperty("command.test") != null) {
        System.out.println("makeFeatureExtractor");
        try {
            Set<String> tags = Collections.EMPTY_SET;
            Map<String, Object> params = new HashMap<>();
            params.put("command", "python src/test/resources/text-feature.py");
            params.put("id", "test");
            Command instance = new Command();
            TextFeature feature = instance.makeFeatureExtractor(tags, params);
            String[] expNames = new String[]{"foo", "bar"};
            double[] expData = new double[]{0.2, 0.3};
            assertArrayEquals(expNames, feature.getFeatureNames());
            assertArrayEquals(expData, toDbA(feature.extractFeatures(null)), 0.0);
        } catch (ExternalCommandException x) {
            x.printStackTrace();
        }
        }
    }
   private double[] toDbA(Feature[] f) {
        double[] d = new double[f.length];
        for(int i = 0; i < f.length; i++) {
            d[i] = f[i].value;
        }
        return d;
    }

}
