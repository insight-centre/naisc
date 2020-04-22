package org.insightcentre.uld.naisc.constraint;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.URIRes;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class BijectiveTest {

    public BijectiveTest() {
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

    private Model m = ModelFactory.createDefaultModel();
    private URIRes r(String s) {
        return new URIRes("file:test#" + s, "dataset");
    }
    
    /**
     * Test of make method, of class Bijective.
     */
    @Test
    public void testMake() {
        System.out.println("make");
        Map<String, Object> params = new HashMap<>();
        Bijective instance = new Bijective();
        Constraint result = instance.make(params);
        result.add(new Alignment(r("a"), r("b"), 1.0, "r", null));
        assert(!result.canAdd(new Alignment(r("a"), r("c"), 1.0, "r", null)));
        assert(!result.canAdd(new Alignment(r("c"), r("b"), 1.0, "r", null)));
        
        params.put("surjection","surjective");
        result = instance.make(params);
        result.add(new Alignment(r("a"), r("b"), 1.0, "r", null));
        assert(!result.canAdd(new Alignment(r("a"), r("c"), 1.0, "r", null)));
        assert(result.canAdd(new Alignment(r("c"), r("b"), 1.0, "r", null)));
        
        params.put("surjection","inverseSurjective");
        result = instance.make(params);
        result.add(new Alignment(r("a"), r("b"), 1.0, "r", null));
        assert(result.canAdd(new Alignment(r("a"), r("c"), 1.0, "r", null)));
        assert(!result.canAdd(new Alignment(r("c"), r("b"), 1.0, "r", null)));
    }

}