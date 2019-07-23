package org.insightcentre.uld.naisc.blocking;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.Some;
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
public class AutomaticTest {

    public AutomaticTest() {
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
     * Test of makeBlockingStrategy method, of class Automatic.
     */
    @Test
    public void testMakeBlockingStrategy() {
        System.out.println("makeBlockingStrategy");
        Map<String, Object> params = new HashMap<>();
                Model model1 = ModelFactory.createDefaultModel();
        model1.add(model1.createStatement(model1.createResource("file:foo1"), model1.createProperty("file:p1"), "e1"));
        model1.add(model1.createStatement(model1.createResource("file:foo2"), model1.createProperty("file:p1"), "e2"));
        model1.add(model1.createStatement(model1.createResource("file:foo1"), model1.createProperty("file:label"), "label1"));
        model1.add(model1.createStatement(model1.createResource("file:foo2"), model1.createProperty("file:label"), "label2"));
        model1.add(model1.createStatement(model1.createResource("file:foo3"), model1.createProperty("file:label"), "label3"));
        model1.add(model1.createStatement(model1.createResource("file:foo4"), model1.createProperty("file:label"), "label4"));
        model1.add(model1.createStatement(model1.createResource("file:foo5"), model1.createProperty("file:label"), "label5"));
        Model model2 = ModelFactory.createDefaultModel();
        model2.add(model2.createStatement(model2.createResource("file:bar1"), model2.createProperty("file:p1"), "e1"));
        model2.add(model2.createStatement(model2.createResource("file:bar2"), model2.createProperty("file:p1"), "e2"));
        model2.add(model2.createStatement(model2.createResource("file:bar5"), model2.createProperty("file:p1"), "e3"));
        model2.add(model2.createStatement(model2.createResource("file:bar1"), model2.createProperty("file:label"), "the label1"));
        model2.add(model2.createStatement(model2.createResource("file:bar2"), model2.createProperty("file:label"), "the label2"));
        model2.add(model2.createStatement(model2.createResource("file:bar3"), model2.createProperty("file:label"), "the label3"));
        model2.add(model2.createStatement(model2.createResource("file:bar4"), model2.createProperty("file:label"), "the label4"));
        model2.add(model2.createStatement(model2.createResource("file:bar5"), model2.createProperty("file:label"), "the label5"));
        Lazy<Analysis> _analysis = Lazy.fromClosure(() -> new DatasetAnalyzer().analyseModel(model1, model2));
        Automatic instance = new Automatic();
        BlockingStrategy strat = instance.makeBlockingStrategy(params, _analysis);
        Iterable<Pair<Resource,Resource>> _results = strat.block(new DatasetImpl(model1), new DatasetImpl(model2));
        Set<Pair<Resource,Resource>> results = new HashSet<>();
        for(Pair<Resource,Resource> p : _results) {
            results.add(p);
        }
        Set<Pair<Resource,Resource>> expResult = new HashSet<>(Arrays.asList(
                new Pair<>(model1.createResource("file:foo1"), model2.createResource("file:bar1")),
                new Pair<>(model1.createResource("file:foo2"), model2.createResource("file:bar2")),
                new Pair<>(model1.createResource("file:foo3"), model2.createResource("file:bar3")),
                new Pair<>(model1.createResource("file:foo3"), model2.createResource("file:bar4")),
                new Pair<>(model1.createResource("file:foo3"), model2.createResource("file:bar5")),
                new Pair<>(model1.createResource("file:foo4"), model2.createResource("file:bar3")),
                new Pair<>(model1.createResource("file:foo4"), model2.createResource("file:bar5")),
                new Pair<>(model1.createResource("file:foo4"), model2.createResource("file:bar4")),
                new Pair<>(model1.createResource("file:foo5"), model2.createResource("file:bar3")),
                new Pair<>(model1.createResource("file:foo5"), model2.createResource("file:bar4")),
                new Pair<>(model1.createResource("file:foo5"), model2.createResource("file:bar5"))
        ));
        assertEquals(expResult, results);
    }
    private static class DatasetImpl implements Dataset {
        private final Model model;

        public DatasetImpl(Model model) {
            this.model = model;
        }

        @Override
        public Option<URL> asEndpoint() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Option<Model> asModel() {
            return new Some<>(model);
        }
        
    }
    
    
    /**
     * Test of makeBlockingStrategy method, of class Automatic.
     */
    @Test
    public void testMakeBlockingStrategyWithoutLabels() {
        System.out.println("makeBlockingStrategy");
        Map<String, Object> params = new HashMap<>();
                Model model1 = ModelFactory.createDefaultModel();
        model1.add(model1.createStatement(model1.createResource("file:foobar1"), model1.createProperty("file:p1"), model1.createResource("adafasdfaf")));
        model1.add(model1.createStatement(model1.createResource("file:foobar2"), model1.createProperty("file:p1"), model1.createResource("adfadfadfadfa")));
        Model model2 = ModelFactory.createDefaultModel();
        model2.add(model2.createStatement(model2.createResource("file:barfoo1"), model2.createProperty("file:p1"), model2.createResource("adfafafdasf")));
        model2.add(model2.createStatement(model2.createResource("file:barfoo2"), model2.createProperty("file:p1"), model2.createResource("adfafdafdsafaf")));
        model2.add(model2.createStatement(model2.createResource("file:foobar2"), model2.createProperty("file:p1"), model2.createResource("adfadfdafawdf")));
        Lazy<Analysis> _analysis = Lazy.fromClosure(() -> new DatasetAnalyzer().analyseModel(model1, model2));
        Automatic instance = new Automatic();
        BlockingStrategy strat = instance.makeBlockingStrategy(params, _analysis);
        Iterable<Pair<Resource,Resource>> _results = strat.block(new DatasetImpl(model1), new DatasetImpl(model2));
        Set<Pair<Resource,Resource>> results = new HashSet<>();
        for(Pair<Resource,Resource> p : _results) {
            results.add(p);
        }
        Set<Pair<Resource,Resource>> expResult = new HashSet<>(Arrays.asList(
                new Pair<>(model1.createResource("file:foobar2"), model2.createResource("file:foobar2")),
                new Pair<>(model1.createResource("file:foobar1"), model2.createResource("file:barfoo1")),
                new Pair<>(model1.createResource("file:foobar1"), model2.createResource("file:barfoo2"))
        ));
        assertEquals(expResult, results);
    }
}