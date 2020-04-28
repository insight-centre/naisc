package org.insightcentre.uld.naisc.blocking;

import java.util.Collections;
import java.util.Map;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Blocking;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;
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
public class AllTest {
    
    public AllTest() {
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
     * Test of makeBlockingStrategy method, of class All.
     */
    @Test
    public void testBlockingStrategy() {
        System.out.println("makeBlockingStrategy");
        
        Map<String, Object> params = Collections.EMPTY_MAP;
        All instance = new All();
        BlockingStrategy strategy = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        Model left = ModelFactory.createDefaultModel();
        Model right = ModelFactory.createDefaultModel();
        left.add(left.createStatement(left.createResource("file:foo"), left.createProperty("file:bar"), left.createResource("file:baz")));
        left.add(left.createStatement(left.createResource(new AnonId()), left.createProperty("file:biz"), left.createResource("file:baz")));
        left.add(left.createStatement(left.createResource("file:foo2"), left.createProperty("file:bar"), left.createResource("file:baz")));
        left.add(left.createStatement(left.createResource("file:foo3"), left.createProperty("file:bar"), left.createResource("file:baz")));
        left.add(left.createStatement(left.createResource("file:foo"), left.createProperty("file:biz"), left.createResource("file:baz")));
        
        right.add(right.createStatement(right.createResource("file:fuzz"), right.createProperty("file:bar"), right.createResource("file:baz")));
        right.add(right.createStatement(right.createResource("file:fuzz2"), right.createProperty("file:bar"), right.createResource("file:baz")));
        right.add(right.createStatement(right.createResource("file:fuzz3"), right.createProperty("file:bar"), right.createResource("file:baz")));
        right.add(right.createStatement(right.createResource(new AnonId()), right.createProperty("file:bar"), right.createResource("file:baz")));
        
        final Iterable<Blocking> block = strategy.block(new ModelDataset(left,"left"), new ModelDataset(right,"right"));
        int i = 0;
        for(Blocking r : block) {
            System.err.println(String.format("%s <-> %s", r.entity1, r.entity2));
            i++; 
        }
        assertEquals(9, i);
    }
    
}
