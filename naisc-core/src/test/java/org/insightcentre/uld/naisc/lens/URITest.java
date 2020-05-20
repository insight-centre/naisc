package org.insightcentre.uld.naisc.lens;

import eu.monnetproject.lang.Language;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.LensResult;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class URITest {

    public URITest() {
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
     * Test of makeLens method, of class Label.
     */
    @Test
    public void testMakeLens() {
        System.out.println("makeLens");
        String tag = null;
        Model model = ModelFactory.createDefaultModel();
        Map<String, Object> params = new HashMap<>();
        URI instance = new URI();
        Lens lens = instance.makeLens(new ModelDataset(model, "model"), params);

        URIRes r1 = new URIRes("http://www.example.com/foo/thisIsAPath#fragmentID", "data");
        URIRes r2 = new URIRes("http://www.example.com/foo/this_is_also_a_path#fragment_id", "data");
        URIRes r3 = new URIRes("http://www.example.com/foo/yet+another+path#fragment+id", "data");
        URIRes r4 = new URIRes("http://www.example.com/foo/a+path+without+a+%23", "data");

        assertEquals(new LensResult(Language.UNDEFINED, Language.UNDEFINED,
                "fragment ID", "fragment id", "uri"), lens.extract(r1, r2).iterator().next());
        assertEquals(new LensResult(Language.UNDEFINED, Language.UNDEFINED,
                "fragment id", "a path without a #", "uri"), lens.extract(r3, r4).iterator().next());

        params.put("location", "fragment");
        params.put("form", "camelCased");
        lens = instance.makeLens(new ModelDataset(model, "model2"), params);

        assertEquals(new LensResult(Language.UNDEFINED, Language.UNDEFINED,
                "fragment ID", "fragment_id", "uri"), lens.extract(r1, r2).iterator().next());
        assert (!lens.extract(r3, r4).iterator().hasNext());

        params.put("location", "endOfPath");
        params.put("form", "urlEncoded");
        lens = instance.makeLens(new ModelDataset(model, "model3"), params);

        assertEquals(new LensResult(Language.UNDEFINED, Language.UNDEFINED,
                "thisIsAPath", "this_is_also_a_path", "uri"), lens.extract(r1, r2).iterator().next());
        assertEquals(new LensResult(Language.UNDEFINED, Language.UNDEFINED,
                "yet another path", "a path without a #", "uri"), lens.extract(r3, r4).iterator().next());

    }

}
