package org.insightcentre.uld.naisc.constraint;

import java.util.HashMap;
import java.util.Map;
import org.insightcentre.uld.naisc.Alignment;
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

    /**
     * Test of make method, of class Bijective.
     */
    @Test
    public void testMake() {
        System.out.println("make");
        Map<String, Object> params = new HashMap<>();
        Bijective instance = new Bijective();
        Constraint result = instance.make(params);
        result = result.add(new Alignment("a", "b", 1.0, "r"));
        assert(!result.canAdd(new Alignment("a", "c", 1.0, "r")));
        assert(!result.canAdd(new Alignment("c", "b", 1.0, "r")));
        
        params.put("surjection","surjective");
        result = instance.make(params);
        result = result.add(new Alignment("a", "b", 1.0, "r"));
        assert(!result.canAdd(new Alignment("a", "c", 1.0, "r")));
        assert(result.canAdd(new Alignment("c", "b", 1.0, "r")));
        
        params.put("surjection","inverseSurjective");
        result = instance.make(params);
        result = result.add(new Alignment("a", "b", 1.0, "r"));
        assert(result.canAdd(new Alignment("a", "c", 1.0, "r")));
        assert(!result.canAdd(new Alignment("c", "b", 1.0, "r")));
    }

}