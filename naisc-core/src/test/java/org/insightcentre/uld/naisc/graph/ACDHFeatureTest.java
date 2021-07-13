package org.insightcentre.uld.naisc.graph;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Feature;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

public class ACDHFeatureTest {
    // This will only work if the ACDH server is running so this test is commented out
    //@Test
    public void testACDH() {
        Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource("file:foo1"), model.createProperty("http://www.w3.org/2000/01/rdf-schema#label"), model.createLiteral("foo", "en"));
        model.add(model.createResource("file:foo1"), model.createProperty("http://www.w3.org/2004/02/skos/core#definition"), model.createLiteral("this is an example definition", "en"));
        model.add(model.createResource("file:foo2"), model.createProperty("http://www.w3.org/2000/01/rdf-schema#label"), model.createLiteral("foo", "en"));
        model.add(model.createResource("file:foo2"), model.createProperty("http://www.w3.org/2004/02/skos/core#definition"), model.createLiteral("this is also an example definition", "en"));

        HashMap<String, Object> config = new HashMap<>();
        config.put("endpoint", "http://localhost:5000/ACDH/ACDH_MWSA_Service/1o/achda-mwsa/scores/");
        GraphFeature feature = new ACDHFeature().makeFeature(new DefaultDatasetLoader.ModelDataset(model, "model", null), config, null, null, null);
        Feature[] feats = feature.extractFeatures(new URIRes("file:foo1", "model"), new URIRes("file:foo2", "model"));
        assert(feats != null);
        assert(feats.length == 5);
        System.out.println(Arrays.toString(feats));
    }
}
