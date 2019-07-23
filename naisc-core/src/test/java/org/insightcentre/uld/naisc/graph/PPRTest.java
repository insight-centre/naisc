package org.insightcentre.uld.naisc.graph;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import org.insightcentre.uld.naisc.util.FastPPR;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Option;
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
public class PPRTest {

    public PPRTest() {
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
     * Test of makeFeature method, of class PPR.
     */
    @Test
    public void testMakeFeature() {
        System.out.println("makeFeature");

        Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource("file:foo1"), model.createProperty("file:p1"), model.createResource("file:foo2"));
        model.add(model.createResource("file:foo2"), model.createProperty("file:p1"), model.createResource("file:foo3"));
        model.add(model.createResource("file:foo3"), model.createProperty("file:p1"), model.createResource("file:foo1"));
        model.add(model.createResource("file:bar1"), model.createProperty("file:p1"), model.createResource("file:bar2"));
        model.add(model.createResource("file:bar2"), model.createProperty("file:p1"), model.createResource("file:bar3"));
        model.add(model.createResource("file:bar3"), model.createProperty("file:p1"), model.createResource("file:bar1"));
        AlignmentSet prealign = new AlignmentSet();
        prealign.add(new Alignment(model.createResource("file:foo1"), model.createResource("file:bar1"), 1.0));
        prealign.add(new Alignment(model.createResource("file:foo1"), model.createResource("file:bar3"), 0.0));
        Dataset sparqlData = new DatasetImpl(model);
        Map<String, Object> params = new HashMap<>();
        Lazy<Analysis> analysis = Lazy.fromClosure(() -> new DatasetAnalyzer().analyseModel(model, model));
        Lazy<AlignmentSet> prelinking = Lazy.fromClosure(() -> prealign);
        PPR instance = new PPR();
        GraphFeature feat = instance.makeFeature(sparqlData, params, analysis, prelinking);
        double[] result = feat.extractFeatures(model.createResource("file:foo2"), model.createResource("file:bar2"));
        double[] expResult = new double[]{0.113};
        assertArrayEquals(expResult, result, 0.01);
    }

    /**
     * Test of buildGraph method, of class PPR.
     */
    @Test
    public void testBuildGraph() {
        System.out.println("buildGraph");
        Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource("file:foo1"), model.createProperty("file:p1"), model.createResource("file:foo2"));
        model.add(model.createResource("file:foo2"), model.createProperty("file:p1"), model.createResource("file:foo3"));
        model.add(model.createResource("file:foo3"), model.createProperty("file:p1"), model.createResource("file:foo1"));
        model.add(model.createResource("file:bar1"), model.createProperty("file:p1"), model.createResource("file:bar2"));
        model.add(model.createResource("file:bar2"), model.createProperty("file:p1"), model.createResource("file:bar3"));
        model.add(model.createResource("file:bar3"), model.createProperty("file:p1"), model.createResource("file:bar1"));
        AlignmentSet prealign = new AlignmentSet();
        prealign.add(new Alignment(model.createResource("file:foo1"), model.createResource("file:bar1"), 1.0));
        prealign.add(new Alignment(model.createResource("file:foo1"), model.createResource("file:bar3"), 0.0));
        Object2IntMap<Resource> identifiers = new Object2IntOpenHashMap<>();
        FastPPR.DirectedGraph result = PPR.buildGraph(model, prealign, identifiers);
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

}