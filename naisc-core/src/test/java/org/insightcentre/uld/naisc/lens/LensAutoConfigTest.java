package org.insightcentre.uld.naisc.lens;

import java.net.URL;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
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
public class LensAutoConfigTest {

    public LensAutoConfigTest() {
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
     * Test of autoConfiguration method, of class LensAutoConfig.
     */
    @Test
    public void testAutoConfiguration() {
        System.out.println("autoConfiguration");
                Model model1 = ModelFactory.createDefaultModel();
        model1.add(model1.createStatement(model1.createResource("file:foo1"), model1.createProperty("file:p1"), "e1"));
        model1.add(model1.createStatement(model1.createResource("file:foo2"), model1.createProperty("file:p1"), "e2"));
        model1.add(model1.createStatement(model1.createResource("file:foo3"), model1.createProperty("file:p1"), "e3"));
        model1.add(model1.createStatement(model1.createResource("file:foo1"), model1.createProperty("file:label"), "label1"));
        model1.add(model1.createStatement(model1.createResource("file:foo2"), model1.createProperty("file:label"), "label2"));
        model1.add(model1.createStatement(model1.createResource("file:foo3"), model1.createProperty("file:label"), "label3"));
        model1.add(model1.createStatement(model1.createResource("file:foo4"), model1.createProperty("file:label"), "label4"));
        model1.add(model1.createStatement(model1.createResource("file:foo5"), model1.createProperty("file:label"), "label5"));
        Model model2 = ModelFactory.createDefaultModel();
        model2.add(model2.createStatement(model2.createResource("file:bar1"), model2.createProperty("file:p2"), "e1"));
        model2.add(model2.createStatement(model2.createResource("file:bar2"), model2.createProperty("file:p2"), "e2"));
        model2.add(model2.createStatement(model2.createResource("file:bar5"), model2.createProperty("file:p2"), "e3"));
        model2.add(model2.createStatement(model2.createResource("file:bar1"), model2.createProperty("file:label"), "the label1"));
        model2.add(model2.createStatement(model2.createResource("file:bar2"), model2.createProperty("file:label"), "the label2"));
        model2.add(model2.createStatement(model2.createResource("file:bar3"), model2.createProperty("file:label"), "the label3"));
        model2.add(model2.createStatement(model2.createResource("file:bar4"), model2.createProperty("file:label"), "the label4"));
        model2.add(model2.createStatement(model2.createResource("file:bar5"), model2.createProperty("file:label"), "the label5"));
        String tag = "tmp";
        Analysis analysis = new DatasetAnalyzer().analyseModel(model1, model2);
        Dataset leftModel = new DatasetImpl(model1);
        Dataset rightModel = new DatasetImpl(model2);
        LensAutoConfig instance = new LensAutoConfig();
        List<Lens> result = instance.autoConfiguration(analysis, leftModel, NaiscListener.DEFAULT);
        assertEquals(3, result.size());
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