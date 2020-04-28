package org.insightcentre.uld.naisc.blocking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
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
public class PreblockingTest {

    public PreblockingTest() {
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
     * Test of preblock method, of class Preblocking.
     */
    @Test
    public void testPreblock() {
        System.out.println("preblock");
        Model model1 = ModelFactory.createDefaultModel();
        model1.add(model1.createStatement(model1.createResource("file:foo1"), model1.createProperty("file:p1"), "foo1"));
        model1.add(model1.createStatement(model1.createResource("file:foo2"), model1.createProperty("file:p1"), "foo2"));
        model1.add(model1.createStatement(model1.createResource("file:foo3"), model1.createProperty("file:p2"), "foo1"));
        model1.add(model1.createStatement(model1.createResource("file:foo4"), model1.createProperty("file:p1"), "foo2"));
        model1.add(model1.createStatement(model1.createResource("file:foo5"), model1.createProperty("file:p1"), "foo3"));
        Model model2 = ModelFactory.createDefaultModel();
        model2.add(model2.createStatement(model2.createResource("file:bar1"), model2.createProperty("file:p1"), "foo1"));
        model2.add(model2.createStatement(model2.createResource("file:bar2"), model2.createProperty("file:p1"), "foo2"));
        model2.add(model2.createStatement(model2.createResource("file:bar3"), model2.createProperty("file:p1"), "foo1"));
        model2.add(model2.createStatement(model2.createResource("file:bar4"), model2.createProperty("file:p1"), "foo4"));
        model2.add(model2.createStatement(model2.createResource("file:bar5"), model2.createProperty("file:p1"), "foo3"));
        Dataset left = new ModelDataset(model1, "model1");
        Dataset right = new ModelDataset(model2, "model2");
        NaiscListener log = NaiscListener.DEFAULT;
        Prelinking instance = new Prelinking(new HashSet<>(Arrays.asList(
                new Pair<>("file:p1", "file:p1"))));
        Set<Pair<Resource, Resource>> expResult = new HashSet<>(Arrays.asList(
                new Pair<>(model1.createResource("file:foo1"), model2.createResource("file:bar1")),
                new Pair<>(model1.createResource("file:foo2"), model2.createResource("file:bar2")),
                new Pair<>(model1.createResource("file:foo1"), model2.createResource("file:bar3")),
                new Pair<>(model1.createResource("file:foo4"), model2.createResource("file:bar2")),
                new Pair<>(model1.createResource("file:foo5"), model2.createResource("file:bar5"))));
        Set<Pair<Resource, Resource>> result = instance.prelink(left, right, log);
        assertEquals(expResult, result);
    }

    /**
     * Test of leftPreblocked method, of class Preblocking.
     */
    @Test
    public void testLeftPreblocked() {
        System.out.println("leftPreblocked");
        Model model1 = ModelFactory.createDefaultModel();
        Model model2 = ModelFactory.createDefaultModel();
        Set<Pair<Resource, Resource>> s = new HashSet<>(Arrays.asList(
                new Pair<>(model1.createResource("file:foo1"), model2.createResource("file:bar1")),
                new Pair<>(model1.createResource("file:foo2"), model2.createResource("file:bar2")),
                new Pair<>(model1.createResource("file:foo1"), model2.createResource("file:bar3")),
                new Pair<>(model1.createResource("file:foo5"), model2.createResource("file:bar5"))));
        Set<Resource> expResult = new HashSet<>(Arrays.asList(
                model1.createResource("file:foo1"),
                model1.createResource("file:foo2"),
                model1.createResource("file:foo1"),
                model1.createResource("file:foo5")));
        Set<Resource> result = Prelinking.leftPrelinked(s);
        assertEquals(expResult, result);
    }

    /**
     * Test of rightPreblocked method, of class Preblocking.
     */
    @Test
    public void testRightPreblocked() {
        System.out.println("rightPreblocked");
        Model model1 = ModelFactory.createDefaultModel();
        Model model2 = ModelFactory.createDefaultModel();
        Set<Pair<Resource, Resource>> s = new HashSet<>(Arrays.asList(
                new Pair<>(model1.createResource("file:foo1"), model2.createResource("file:bar1")),
                new Pair<>(model1.createResource("file:foo2"), model2.createResource("file:bar2")),
                new Pair<>(model1.createResource("file:foo1"), model2.createResource("file:bar3")),
                new Pair<>(model1.createResource("file:foo5"), model2.createResource("file:bar5"))));
        Set<Resource> expResult = new HashSet<>(Arrays.asList(
                model1.createResource("file:bar1"),
                model1.createResource("file:bar2"),
                model1.createResource("file:bar3"),
                model1.createResource("file:bar5")));
        Set<Resource> result = Prelinking.rightPrelinked(s);
        assertEquals(expResult, result);
    }

}
