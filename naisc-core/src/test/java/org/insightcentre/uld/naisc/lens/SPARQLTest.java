package org.insightcentre.uld.naisc.lens;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class SPARQLTest {

    public SPARQLTest() {
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
     * Test of makeLens method, of class SPARQL.
     */
    @Test
    public void testMakeLens() {
        System.out.println("makeLens");
        String tag = null;
        Model model = ModelFactory.createDefaultModel();
        Map<String, Object> params = new HashMap<>();
        params.put("query", "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
"SELECT ?label1 ?label2 WHERE { $entity1 rdfs:label ?label1 . $entity2\n" +
" rdfs:label ?label2 . }");
        SPARQL instance = new SPARQL();
        final Resource res = model.createResource("http://www.example.com/foo");
        final Resource res2 = model.createResource("http://www.example.com/foo2");
        final Resource res3 = model.createResource("http://www.example.com/foo3");
        final Resource res4 = model.createResource("http://www.example.com/foo4");
        final Resource res5 = model.createResource("http://www.example.com/foo5");
        
        model.add(res, 
                model.createProperty(Label.RDFS_LABEL), 
                model.createLiteral("english", "en"));
        
        model.add(res2, 
                model.createProperty(Label.RDFS_LABEL), 
                model.createLiteral("deutsch", "de"));
        
        model.add(res3, 
                model.createProperty(Label.RDFS_LABEL), 
                model.createLiteral("???"));
        
        model.add(res4, 
                model.createProperty(Label.RDFS_LABEL), 
                model.createLiteral("more english", "en"));
        
        model.add(res5, 
                model.createProperty(Label.SKOS_PREFLABEL), 
                model.createLiteral("???"));
        
        Lens lens = instance.makeLens(new ModelDataset(model, "model"), params);
        assert(!lens.extract(URIRes.fromJena(res, "model"), URIRes.fromJena(res2, "model")).isEmpty());
        assert(!lens.extract(URIRes.fromJena(res, "model"), URIRes.fromJena(res3, "model")).isEmpty());
        assert(!lens.extract(URIRes.fromJena(res, "model"), URIRes.fromJena(res4, "model")).isEmpty());
        assert(lens.extract(URIRes.fromJena(res, "model"), URIRes.fromJena(res5, "model")).isEmpty());
        
    }

}