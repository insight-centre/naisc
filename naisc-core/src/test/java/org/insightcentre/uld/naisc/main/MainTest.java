package org.insightcentre.uld.naisc.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.insightcentre.uld.naisc.Alignment.SKOS_EXACT_MATCH;
import static org.insightcentre.uld.naisc.lens.Label.RDFS_LABEL;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.NONE;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.util.None;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class MainTest {
    
    public MainTest() {
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
     * Test of execute method, of class Main.
     */
    @Test
    public void testExecute() throws Exception {
        System.out.println("execute");
        File leftFile = new File("src/test/resources/left.nt");
        File rightFile = new File("src/test/resources/right.nt");
        File configuration = new File("src/test/resources/simple.json");
        File outputFile = null;
        boolean outputXML = false;
        Main.execute("test", leftFile, rightFile, configuration, outputFile, new None<>(), outputXML, NONE, new DefaultDatasetLoader());
    }


    @Test
    public void testExecuteWithOutput() throws Exception {
        System.out.println("executeWithOutput");
        Model leftModel = ModelFactory.createDefaultModel();
        leftModel.add(leftModel.createStatement(leftModel.createResource("http://www.example.com/foo"),
            leftModel.createProperty(RDFS_LABEL), leftModel.createLiteral("foo", "en")));
        Model rightModel = ModelFactory.createDefaultModel();
        rightModel.add(rightModel.createStatement(rightModel.createResource("file:foo"),
            rightModel.createProperty(RDFS_LABEL), rightModel.createLiteral("foo", "en")));
        Dataset leftDataset = new DefaultDatasetLoader.ModelDataset(leftModel, "left");
        Dataset rightDataset = new DefaultDatasetLoader.ModelDataset(rightModel, "right");
        Configuration config = new Configuration(
            new Configuration.BlockingStrategyConfiguration("blocking.All", new HashMap<>()),
            Arrays.asList(new Configuration.LensConfiguration("lens.Label", new HashMap<>())),
            new ArrayList<>(),
            Arrays.asList(new Configuration.TextFeatureConfiguration("feature.BasicString", new HashMap<>(), Collections.EMPTY_SET)),
            Arrays.asList(new Configuration.ScorerConfiguration("scorer.Average", new HashMap<>(), null)),
            new Configuration.MatcherConfiguration("matcher.Threshold", new HashMap<>()),
            "test case", null);
        config.includeFeatures = true;
        config.noPrematching = true;
        AlignmentSet align = Main.execute("test", leftDataset, rightDataset, config, new None<>(), ExecuteListeners.NONE,
            null, null, new DefaultDatasetLoader());
        String text = new ObjectMapper().writeValueAsString(align);
        System.out.println(text);
        assert(text.contains("containment-label"));
    }

    @Test
    public void testExecuteIgnorePreexisting() throws Exception {
        System.out.println("executeWithOutput");
        Model leftModel = ModelFactory.createDefaultModel();
        leftModel.add(leftModel.createStatement(leftModel.createResource("http://www.example.com/foo"),
            leftModel.createProperty(RDFS_LABEL), leftModel.createLiteral("foo", "en")));
        Model rightModel = ModelFactory.createDefaultModel();
        rightModel.add(rightModel.createStatement(rightModel.createResource("file:foo"),
            rightModel.createProperty(RDFS_LABEL), rightModel.createLiteral("foo", "en")));
        leftModel.add(leftModel.createResource("http://www.example.com/foo"),
            leftModel.createProperty(SKOS_EXACT_MATCH),
            leftModel.createResource("file:foo"));
        Dataset leftDataset = new DefaultDatasetLoader.ModelDataset(leftModel, "left");
        Dataset rightDataset = new DefaultDatasetLoader.ModelDataset(rightModel, "right");
        Configuration config = new Configuration(
            new Configuration.BlockingStrategyConfiguration("blocking.All", new HashMap<>()),
            Arrays.asList(new Configuration.LensConfiguration("lens.Label", new HashMap<>())),
            new ArrayList<>(),
            Arrays.asList(new Configuration.TextFeatureConfiguration("feature.BasicString", new HashMap<>(), Collections.EMPTY_SET)),
            Arrays.asList(new Configuration.ScorerConfiguration("scorer.Average", new HashMap<>(), null)),
            new Configuration.MatcherConfiguration("matcher.Threshold", new HashMap<>()),
            "test case", null);
        config.includeFeatures = true;
        config.ignorePreexisting = true;
        AlignmentSet align = Main.execute("test", leftDataset, rightDataset, config, new None<>(), ExecuteListeners.NONE,
            null, null, new DefaultDatasetLoader());
        assertEquals(0, align.size());
    }}
