package org.insightcentre.uld.naisc.blocking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.Blocking;
import org.insightcentre.uld.naisc.BlockingStrategy;
import static org.insightcentre.uld.naisc.lens.Label.RDFS_LABEL;

import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;
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
public class PathTest {

    public PathTest() {
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
     * Test of makeBlockingStrategy method, of class Path.
     */
    @Test
    public void testMakeBlockingStrategy() {
        System.out.println("makeBlockingStrategy");
        Map<String, Object> params = new HashMap<>();
        params.put("maxMatches", "2");
        Path instance = new Path();
        Model left = ModelFactory.createDefaultModel();
        Model right = ModelFactory.createDefaultModel();
        
        left.add(left.createStatement(left.createResource("file:foo"), left.createProperty(RDFS_LABEL), left.createLiteral("cat", "en")));
        left.add(left.createStatement(left.createResource(new AnonId()), left.createProperty(RDFS_LABEL), left.createLiteral("dog", "en")));
        left.add(left.createStatement(left.createResource("file:foo2"), left.createProperty(RDFS_LABEL), left.createLiteral("dog", "en")));
        left.add(left.createStatement(left.createResource("file:foo2"), left.createProperty(Alignment.SKOS_EXACT_MATCH), left.createResource("file:foo")));
        left.add(left.createStatement(left.createResource("file:foo3"), left.createProperty(RDFS_LABEL), left.createLiteral("cat", "ga")));
        left.add(left.createStatement(left.createResource("file:foo3"), left.createProperty("file:foo"), left.createLiteral("cat", "en")));
        left.add(left.createStatement(left.createResource("file:foo3"), left.createProperty(Alignment.SKOS_EXACT_MATCH), left.createResource("file:foo5")));
        left.add(left.createStatement(left.createResource("file:foo4"), left.createProperty(RDFS_LABEL), left.createLiteral("bee", "en")));
        
        right.add(right.createStatement(right.createResource("file:fuzz"), right.createProperty(RDFS_LABEL), right.createLiteral("cat", "en")));
        right.add(right.createStatement(right.createResource("file:fuzz2"), right.createProperty(RDFS_LABEL), right.createLiteral("dog", "en")));
        right.add(right.createStatement(right.createResource("file:fuzz3"), right.createProperty(RDFS_LABEL), right.createLiteral("dog house", "en")));
        right.add(right.createStatement(right.createResource("file:fuzz4"), right.createProperty(RDFS_LABEL), right.createLiteral("bee", "en")));
        right.add(right.createStatement(right.createResource("file:fuzz4"), right.createProperty(Alignment.SKOS_EXACT_MATCH), right.createResource("fuzz5")));
        right.add(right.createStatement(right.createResource("file:fuzz5"), right.createProperty(Alignment.SKOS_EXACT_MATCH), right.createResource("fuzz6")));
        
        BlockingStrategy strategy = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        Iterable<Blocking> result = strategy.block(new ModelDataset(left, "left"), new ModelDataset(right, "right"));
        Map<URIRes,List<URIRes>> result2 = new HashMap<>();
        for(Blocking r : result) {
            if(!result2.containsKey(r.entity1))
                result2.put(r.entity1, new ArrayList<>());
            result2.get(r.entity1).add(r.entity2);
        }
        assertEquals(2, result2.get(new URIRes("file:foo","left")).size());
        assertEquals(2, result2.get(new URIRes("file:foo2","left")).size());
        assertEquals(2, result2.get(new URIRes("file:foo4","left")).size());
        
        
    }

}