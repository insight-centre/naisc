package org.insightcentre.uld.naisc.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Feature;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
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
public class PropertyOverlapTest {

    public PropertyOverlapTest() {
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
     * Test of makeFeature method, of class PropertyOverlap.
     */
    @Test
    public void testMakeFeature() {
        System.out.println("makeFeature");
        Model model = ModelFactory.createDefaultModel();
        Map<String, Object> params = new HashMap<>();
        PropertyOverlap instance = new PropertyOverlap();
        GraphFeature feature = instance.makeFeature(new ModelDataset(model, "model"), params, null, null, ExecuteListeners.NONE);

        Resource res1 = model.createResource("http://www.example.com/res1");
        Resource res2 = model.createResource("http://www.example.com/res2");
        res1.addProperty(model.createProperty("http://www.example.com/bar"), model.createLiteral("foo", "en"));
        res1.addProperty(model.createProperty("http://www.example.com/foo"), model.createLiteral("foo"));
        res1.addProperty(model.createProperty("http://www.example.com/bar"), model.createLiteral("foo"));
        res1.addProperty(model.createProperty("http://www.example.com/foo"), model.createResource("file:foo"));

        res2.addProperty(model.createProperty("http://www.example.com/foo"), model.createLiteral("foo"));
        res2.addProperty(model.createProperty("http://www.example.com/bar"), model.createLiteral("foo"));
        res2.addProperty(model.createProperty("http://www.example.com/baz"), model.createLiteral("foo"));

        Feature[] result = feature.extractFeatures(URIRes.fromJena(res1, "model"), URIRes.fromJena(res2, "model"));

        assertArrayEquals(new double[]{ 4.0 / 7.0, 2.0 / 5.0 }, toDbA(result), 0.000001);

        
        params.put("properties", new HashSet<String>() {{ add("http://www.example.com/foo"); }});
        
        feature = instance.makeFeature(new ModelDataset(model, "model"), params, null, null, ExecuteListeners.NONE);
        result = feature.extractFeatures(URIRes.fromJena(res1, "model"), URIRes.fromJena(res2, "model"));

        assertArrayEquals(new double[]{ 2.0 / 3.0, 1.0 / 2.0 }, toDbA(result), 0.000001);
    }
    private double[] toDbA(Feature[] f) {
        double[] d = new double[f.length];
        for(int i = 0; i < f.length; i++) {
            d[i] = f[i].value;
        }
        return d;
    }

}
