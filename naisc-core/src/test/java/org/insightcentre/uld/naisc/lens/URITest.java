package org.insightcentre.uld.naisc.lens;

import eu.monnetproject.lang.Language;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.main.ModelDataset;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Lazy;
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
        Lens lens = instance.makeLens(tag, new ModelDataset(model), params, Lazy.fromClosure(() -> null));

        Resource r1 = model.createResource("http://www.example.com/foo/thisIsAPath#fragmentID");
        Resource r2 = model.createResource("http://www.example.com/foo/this_is_also_a_path#fragment_id");
        Resource r3 = model.createResource("http://www.example.com/foo/yet+another+path#fragment+id");
        Resource r4 = model.createResource("http://www.example.com/foo/a+path+without+a+%23");

        assertEquals(new LangStringPair(Language.UNDEFINED, Language.UNDEFINED,
                "fragment ID", "fragment id"), lens.extract(r1, r2).get());
        assertEquals(new LangStringPair(Language.UNDEFINED, Language.UNDEFINED,
                "fragment id", "a path without a #"), lens.extract(r3, r4).get());

        params.put("location", "fragment");
        params.put("form", "camelCased");
        lens = instance.makeLens(tag, new ModelDataset(model), params, Lazy.fromClosure(() -> null));

        assertEquals(new LangStringPair(Language.UNDEFINED, Language.UNDEFINED,
                "fragment ID", "fragment_id"), lens.extract(r1, r2).get());
        assert (!lens.extract(r3, r4).has());

        params.put("location", "endOfPath");
        params.put("form", "urlEncoded");
        lens = instance.makeLens(tag, new ModelDataset(model), params, Lazy.fromClosure(() -> null));

        assertEquals(new LangStringPair(Language.UNDEFINED, Language.UNDEFINED,
                "thisIsAPath", "this_is_also_a_path"), lens.extract(r1, r2).get());
        assertEquals(new LangStringPair(Language.UNDEFINED, Language.UNDEFINED,
                "yet another path", "a path without a #"), lens.extract(r3, r4).get());

    }

}
