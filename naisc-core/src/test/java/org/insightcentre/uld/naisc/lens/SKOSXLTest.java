package org.insightcentre.uld.naisc.lens;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

public class SKOSXLTest {
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
     * Test of makeLens method, of class Label.
     */
    @Test
    public void testMakeLens() {
        System.out.println("makeLens");
        String tag = null;
        Model model = ModelFactory.createDefaultModel();
        Map<String, Object> params = new HashMap<>();
        SKOSXL instance = new SKOSXL();
        Dataset dataset = new DefaultDatasetLoader.ModelDataset(model, "model", null);
        Lens lens = instance.makeLens(dataset, params);
        final URIRes res = new URIRes("http://www.example.com/foo", "model");
        final URIRes res2 = new URIRes("http://www.example.com/foo2", "model");
        final URIRes res3 = new URIRes("http://www.example.com/foo3", "model");
        final URIRes res4 = new URIRes("http://www.example.com/foo4", "model");
        final URIRes res5 = new URIRes("http://www.example.com/foo5", "model");

        model.add(res.toJena(dataset),
                model.createProperty("http://www.w3.org/2008/05/skos-xl#prefLabel"),
                model.createResource().addProperty(model.createProperty("http://www.w3.org/2008/05/skos-xl#literalForm"),
                    model.createLiteral("english", "en")));
        model.add(res.toJena(dataset),
                model.createProperty(Label.RDFS_LABEL),
                model.createLiteral("english", "en"));

        model.add(res2.toJena(dataset),
                model.createProperty("http://www.w3.org/2008/05/skos-xl#prefLabel"),
                model.createResource().addProperty(model.createProperty("http://www.w3.org/2008/05/skos-xl#literalForm"),
                    model.createLiteral("deutsch", "de")));

        model.add(res3.toJena(dataset),
                model.createProperty("http://www.w3.org/2008/05/skos-xl#prefLabel"),
                model.createResource().addProperty(model.createProperty("http://www.w3.org/2008/05/skos-xl#literalForm"),
                model.createLiteral("???")));

        model.add(res4.toJena(dataset),
                model.createProperty("http://www.w3.org/2008/05/skos-xl#prefLabel"),
                model.createResource().addProperty(model.createProperty("http://www.w3.org/2008/05/skos-xl#literalForm"),
                model.createLiteral("more english", "en")));

        model.add(res5.toJena(dataset),
                model.createProperty("http://www.w3.org/2008/05/skos-xl#altLabel"),
                model.createResource().addProperty(model.createProperty("http://www.w3.org/2008/05/skos-xl#literalForm"),
                model.createLiteral("???")));

        assert(!lens.extract(res, res2).iterator().hasNext());
        assert(lens.extract(res, res3).iterator().hasNext());
        assert(lens.extract(res, res4).iterator().hasNext());
        assert(lens.extract(res, res5).iterator().hasNext());

        params.put("language", "en");
        lens = instance.makeLens(new DefaultDatasetLoader.ModelDataset(model, "model", null), params);


        assert(!lens.extract(res, res2).iterator().hasNext());
        assert(lens.extract(res, res3).iterator().hasNext());
        assert(lens.extract(res, res4).iterator().hasNext());
        assert(lens.extract(res, res5).iterator().hasNext());

        params.remove("language");
        params.put("leftLabel", Label.RDFS_LABEL);
        lens = instance.makeLens(new DefaultDatasetLoader.ModelDataset(model, "model", null), params);


        assert(!lens.extract(res, res2).iterator().hasNext());
        assert(lens.extract(res, res3).iterator().hasNext());
        assert(lens.extract(res, res4).iterator().hasNext());
        //assert(lens.extract(res, res5).has());



    }

}
