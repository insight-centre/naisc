package org.insightcentre.uld.naisc.graph;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.util.ExternalCommandException;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;
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
            Dataset sparqlData = new Dataset() {
                @Override
                public Option<Model> asModel() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Option<URL> asEndpoint() {
                    try {
                        return new Some<>(new URL("http://www.example.com"));
                    } catch (MalformedURLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
            Map<String, Object> params = new HashMap<>();
            params.put("command", "python3 src/test/resources/test-graph.py");
            params.put("id", "test");
            Command instance = new Command();
            GraphFeature feature = instance.makeFeature(sparqlData, params);
            double[] result = feature.extractFeatures(model.createResource("http://www.example.com/example"),
                    model.createResource("http://www.example.com/anotherExample"));
            double[] expResult = new double[]{"http://www.example.com/example".length(),
                "http://www.example.com/anotherExample".length()
            };
            assertArrayEquals(new String[]{"length1", "length2"}, feature.getFeatureNames());
            assertArrayEquals(expResult, result, 0.0);
        } catch (ExternalCommandException x) {
            x.printStackTrace();
        }
        }
    }

}
