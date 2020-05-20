package org.insightcentre.uld.naisc.graph;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Feature;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
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
     * Test of makeFeature method, of class Command.
     */
    @Test
    public void testMakeFeature() {
        if(System.getProperty("command.test") != null) {
        try {
            System.out.println("makeFeature");
            final Model model = ModelFactory.createDefaultModel();
            Dataset sparqlData = new ModelDataset(model, "sparql");
            Map<String, Object> params = new HashMap<>();
            params.put("command", "python3 src/test/resources/test-graph.py");
            params.put("id", "test");
            Command instance = new Command();
            GraphFeature feature = instance.makeFeature(sparqlData, params, null, null, ExecuteListeners.NONE);
            Feature[] result = feature.extractFeatures(new URIRes("http://www.example.com/example", "sparql"),
                    new URIRes("http://www.example.com/anotherExample", "sparql"));
            double[] expResult = new double[]{"http://www.example.com/example".length(),
                "http://www.example.com/anotherExample".length()
            };
            //assertArrayEquals(new String[]{"length1", "length2"}, feature.getFeatureNames());
            assertArrayEquals(expResult, toDbA(result), 0.0);
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
