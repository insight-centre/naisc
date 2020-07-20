package org.insightcentre.uld.naisc.blocking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.BlockingStrategy;
import static org.insightcentre.uld.naisc.lens.Label.RDFS_LABEL;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
import org.insightcentre.uld.naisc.util.Lazy;
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
public class IDMatchTest {

    public IDMatchTest() {
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
     * Test of makeBlockingStrategy method, of class IDMatch.
     */
    @Test
    public void testMakeBlockingStrategy() {
        System.out.println("makeBlockingStrategy");
        Map<String, Object> params = new HashMap<String, Object>();
        IDMatch instance = new IDMatch();
        
        Model left = ModelFactory.createDefaultModel();
        Model right = ModelFactory.createDefaultModel();
        
        left.add(left.createStatement(left.createResource("http://www.example.com/foo/bar#baz"), left.createProperty(RDFS_LABEL), left.createLiteral("cat", "en")));
        left.add(left.createStatement(left.createResource(new AnonId()), left.createProperty(RDFS_LABEL), left.createLiteral("dog", "en")));
        
        right.add(right.createStatement(right.createResource("http://www.example.com/foo/bar#baz"), right.createProperty(RDFS_LABEL), left.createLiteral("cat", "en")));
        right.add(right.createStatement(right.createResource("http://www.beispiel.de/path/to#baz"), right.createProperty(RDFS_LABEL), left.createLiteral("dog", "en")));
        right.add(right.createStatement(right.createResource("http://www.beispiel.de/path/bar#baz"), right.createProperty(RDFS_LABEL), left.createLiteral("dog house", "en")));
        right.add(right.createStatement(right.createResource("http://www.beispiel.de/foo/bar#baz"), right.createProperty(RDFS_LABEL), left.createLiteral("dog house", "en")));
        
        BlockingStrategy strategy = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        assertEquals(3, count(strategy.block(new ModelDataset(left, "left"), new ModelDataset(right, "right"))));
        
        params.put("method", "exact");
        strategy = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        assertEquals(1, count(strategy.block(new ModelDataset(left, "left"), new ModelDataset(right, "right"))));
        
        params.put("method", "fragment");
        strategy = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        assertEquals(4, count(strategy.block(new ModelDataset(left, "left"), new ModelDataset(right, "right"))));
        
        params.put("method", "exact");
        params.put("leftNamespace", "http://www.example.com/");
        params.put("rightNamespace", "http://www.beispiel.de");
        strategy = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        assertEquals(1, count(strategy.block(new ModelDataset(left, "left"), new ModelDataset(right, "right"))));
    }

       
    private int count(Iterable i) {
        int n = 0;
        Iterator i2 = i.iterator();
        while(i2.hasNext()) {
            i2.next();
            n++;
        }
        return n;
    }
}