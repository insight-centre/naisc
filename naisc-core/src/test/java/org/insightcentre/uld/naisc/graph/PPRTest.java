package org.insightcentre.uld.naisc.graph;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
import org.insightcentre.uld.naisc.util.FastPPR;
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
        prealign.add(new Alignment(new URIRes("file:foo1", "model"), new URIRes("file:bar1", "model"), 1.0));
        prealign.add(new Alignment(new URIRes("file:foo1", "model"), new URIRes("file:bar3", "model"), 0.0));
        Dataset sparqlData = new ModelDataset(model, "model");
        Map<String, Object> params = new HashMap<>();
        Lazy<Analysis> analysis = Lazy.fromClosure(() -> new DatasetAnalyzer().analyseModel(new ModelDataset(model, "model"), new ModelDataset(model, "model")));
        PPR instance = new PPR();
        GraphFeature feat = instance.makeFeature(sparqlData, params, analysis, prealign, ExecuteListeners.NONE);
        Feature[] result = feat.extractFeatures(new URIRes("file:foo2", "model"), new URIRes("file:bar2", "model"));
        double[] expResult = new double[]{0.113};
        assertArrayEquals(expResult, toDbA(result), 0.01);
    }
   private double[] toDbA(Feature[] f) {
        double[] d = new double[f.length];
        for(int i = 0; i < f.length; i++) {
            d[i] = f[i].value;
        }
        return d;
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
        prealign.add(new Alignment(new URIRes("file:foo1", "model"), new URIRes("file:bar1", "model"), 1.0));
        prealign.add(new Alignment(new URIRes("file:foo1", "model"), new URIRes("file:bar3", "model"), 0.0));
        Object2IntMap<Resource> identifiers = new Object2IntOpenHashMap<>();
        FastPPR.DirectedGraph result = PPR.buildGraph(new ModelDataset(model, "model"), prealign, identifiers);
    }


}
