package org.insightcentre.uld.naisc.scorer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import libsvm.svm_node;
import libsvm.svm_problem;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.FeatureSetWithScore;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.ScoreResult;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.util.StringPair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class LibSVMTest {

    public LibSVMTest() {
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
     * Test of id method, of class LibSVM.
     */
    @Test
    public void testId() {
        System.out.println("id");
        LibSVM instance = new LibSVM();
        String expResult = "libsvm";
        String result = instance.id();
        assertEquals(expResult, result);
    }

    /**
     * Test of makeInstances method, of class LibSVM.
     */
    @Test
    public void testMakeInstances() {
        System.out.println("makeInstances");
        FeatureSet example = new FeatureSet(new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.5});
        String datasetName = "test";
        String[] featNames = new String[]{"foobar"};
        svm_problem result = LibSVM.makeInstances(example);
    }

    /**
     * Test of analyzeFeatures method, of class LibSVM.
     */
    @Test
    public void testAnalyzeFeatures() {
        System.out.println("analyzeFeatures");
        FeatureSet example = new FeatureSet(new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.5});
        String datasetName = "test";
        String[] featNames = new String[]{"foobar"};
        svm_problem instances = LibSVM.makeInstances(example);
        instances.x = new svm_node[][]{
            LibSVM.buildInstance(example, 1.0, instances).x,
            LibSVM.buildInstance(example, 0.0, instances).x
        };
        instances.y = new double[]{1.0, 0.0};
        LibSVM.analyzeFeatures(instances);
    }

    /**
     * Test of train method, of class LibSVM.
     */
    @Test
    public void testTrain() throws IOException {
        System.out.println("train");
        List<FeatureSetWithScore> dataset = Arrays.asList(new FeatureSetWithScore(1.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{1.0}, "id1", "id2"),
                new FeatureSetWithScore(0.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.0}, "id1", "id2"));
        Map<String, Object> params = new HashMap<>();
        File tmpFile = File.createTempFile("libsvm", ".model");
        params.put("modelFile", tmpFile.getAbsolutePath());
        tmpFile.deleteOnExit();
        LibSVM instance = new LibSVM();
        instance.makeTrainer(params, Alignment.SKOS_EXACT_MATCH, tmpFile);
    }

    /**
     * Test of load method, of class LibSVM.
     */
    @Test
    public void testLoad() throws Exception {
        // Skip
    }

    /**
     * Test of buildInstance method, of class LibSVM.
     */
    @Test
    public void testBuildInstance() {
        System.out.println("buildInstance");
        FeatureSet fss = new FeatureSet(new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.5});
        String datasetName = "test";
        String[] featNames = new String[]{"foobar"};
        double score = 0.0;
        svm_problem instances = LibSVM.makeInstances(fss);
        LibSVM.buildInstance(fss, score, instances);
    }

    @Test
    public void testClassify() throws Exception {
        
//        List<FeatureSetWithScore> dataset = Arrays.asList(
//                new FeatureSetWithScore(1.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{1.0}, "id1", "id2"),
//                new FeatureSetWithScore(1.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.8}, "id1", "id2"),
//                new FeatureSetWithScore(1.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.6}, "id1", "id2"),
//                new FeatureSetWithScore(1.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.5}, "id1", "id2"),
//                new FeatureSetWithScore(-1.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.4}, "id1", "id2"),
//                new FeatureSetWithScore(-1.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.2}, "id1", "id2"),
//                new FeatureSetWithScore(-1.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.0}, "id1", "id2"));
        List<FeatureSetWithScore> dataset = new ArrayList<>();
        Random random = new Random();
        for(int i = 0; i < 100; i++) {
            double r = random.nextDouble();
            dataset.add(new FeatureSetWithScore(r, new StringPair[]{new StringPair("foo", "bar")}, new double[]{r}, "id1", "id2"));
        }
        Map<String, Object> params = new HashMap<>();
        File tmpFile = File.createTempFile("libsvm", ".model");
        params.put("modelFile", tmpFile.getAbsolutePath());
        tmpFile.deleteOnExit();
        LibSVM instance = new LibSVM();
        Scorer result = instance.makeTrainer(params, Alignment.SKOS_EXACT_MATCH, tmpFile).get().train(dataset, NaiscListener.DEFAULT);
        ScoreResult sim1 = result.similarity(new FeatureSetWithScore(1.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{1.0}, "id1", "id2")).get(0);
        System.err.println(sim1);
        ScoreResult sim6 = result.similarity(new FeatureSetWithScore(0.0, new StringPair[]{new StringPair("foo", "bar")}, new double[]{0.0}, "id1", "id2")).get(0);
        System.err.println(sim6);
        assert(sim1.getProbability() > sim6.getProbability());
    }

}
