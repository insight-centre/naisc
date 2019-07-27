package org.insightcentre.uld.naisc.graph;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
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
     * Test of makeFeature method, of class Automatic.
     */
    @Test
    public void testMakeFeature() {
        System.out.println("makeFeature");
        Model lmodel = ModelFactory.createDefaultModel();
        lmodel.add(lmodel.createResource("file:foo1"), lmodel.createProperty("file:p1"), lmodel.createResource("file:foo2"));
        lmodel.add(lmodel.createResource("file:foo2"), lmodel.createProperty("file:p1"), lmodel.createResource("file:foo3"));
        lmodel.add(lmodel.createResource("file:foo3"), lmodel.createProperty("file:p1"), lmodel.createResource("file:foo1"));
        lmodel.add(lmodel.createResource("file:foo1"), lmodel.createProperty("file:p2"), "true");
        lmodel.add(lmodel.createResource("file:foo2"), lmodel.createProperty("file:p2"), "false");
        lmodel.add(lmodel.createResource("file:foo3"), lmodel.createProperty("file:p2"), "maybe");

        Model rmodel = ModelFactory.createDefaultModel();
        rmodel.add(rmodel.createResource("file:bar1"), rmodel.createProperty("file:p1"), rmodel.createResource("file:bar2"));
        rmodel.add(rmodel.createResource("file:bar2"), rmodel.createProperty("file:p1"), rmodel.createResource("file:bar3"));
        rmodel.add(rmodel.createResource("file:bar3"), rmodel.createProperty("file:p1"), rmodel.createResource("file:bar1"));
        rmodel.add(rmodel.createResource("file:bar1"), rmodel.createProperty("file:p2"), "true");
        rmodel.add(rmodel.createResource("file:bar2"), rmodel.createProperty("file:p2"), "true");
        rmodel.add(rmodel.createResource("file:bar3"), rmodel.createProperty("file:p2"), "true");

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
        for (int i = 0; i < 10; i++) {
            prealign.add(new Alignment(model.createResource("file:foo" + i), model.createResource("file:bar" + i), 1.0));

        }
        Dataset sparqlData = new ModelDataset(model);
        Map<String, Object> params = new HashMap<>();
        Lazy<Analysis> analysis = Lazy.fromClosure(() -> new DatasetAnalyzer().analyseModel(new ModelDataset(lmodel), new ModelDataset(rmodel)));
        Lazy<AlignmentSet> prelinking = Lazy.fromClosure(() -> prealign);
        GraphFeature feat = new Automatic().makeFeature(sparqlData, params, analysis, prelinking);
        double[] result = feat.extractFeatures(lmodel.createResource("file:foo2"), rmodel.createResource("file:bar2"));
        double[] expResult = new double[]{0.0, 0.242};
        assertArrayEquals(expResult, result, 0.01);
        result = feat.extractFeatures(lmodel.createResource("file:foo1"), rmodel.createResource("file:bar1"));
        expResult = new double[]{1.0, 0.242};
        assertArrayEquals(expResult, result, 0.01);
    }
}
