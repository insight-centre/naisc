package org.insightcentre.uld.naisc.blocking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.lens.Label;
import static org.insightcentre.uld.naisc.lens.Label.RDFS_LABEL;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
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
public class LabelMatchTest {

    public LabelMatchTest() {
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
     * Test of makeBlockingStrategy method, of class LabelMatch.
     */
    @Test
    public void testMakeBlockingStrategy() {
        System.out.println("makeBlockingStrategy");
        Map<String, Object> params = new HashMap<>();
        params.put("property", Label.RDFS_LABEL);
        LabelMatch instance = new LabelMatch();
        Model left = ModelFactory.createDefaultModel();
        Model right = ModelFactory.createDefaultModel();
        
        left.add(left.createStatement(left.createResource("file:foo"), left.createProperty(RDFS_LABEL), left.createLiteral("cat", "en")));
        left.add(left.createStatement(left.createResource(new AnonId()), left.createProperty(RDFS_LABEL), left.createLiteral("dog", "en")));
        left.add(left.createStatement(left.createResource("file:foo2"), left.createProperty(RDFS_LABEL), left.createLiteral("dog", "en")));
        left.add(left.createStatement(left.createResource("file:foo3"), left.createProperty(RDFS_LABEL), left.createLiteral("cat", "ga")));
        left.add(left.createStatement(left.createResource("file:foo3"), left.createProperty("file:foo"), left.createLiteral("cat", "en")));
        
        right.add(right.createStatement(right.createResource("file:fuzz"), right.createProperty(RDFS_LABEL), left.createLiteral("cat", "en")));
        right.add(right.createStatement(right.createResource("file:fuzz2"), right.createProperty(RDFS_LABEL), left.createLiteral("dog", "en")));
        right.add(right.createStatement(right.createResource("file:fuzz3"), right.createProperty(RDFS_LABEL), left.createLiteral("dog house", "en")));
        
        BlockingStrategy strategy = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        assertEquals(3, count(strategy.block(new ModelDataset(left, "left"), new ModelDataset(right, "right"))));
        
        params.put("language", "en");
        strategy = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        assertEquals(2, count(strategy.block(new ModelDataset(left, "left"), new ModelDataset(right, "right"))));

        
        params.put("mode", "lenient");
        strategy = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        assertEquals(3, count(strategy.block(new ModelDataset(left, "left"), new ModelDataset(right, "right"))));
        
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