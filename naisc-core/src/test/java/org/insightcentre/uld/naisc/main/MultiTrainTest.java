package org.insightcentre.uld.naisc.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.EvaluationSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class MultiTrainTest {

    public MultiTrainTest() {
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
     * Test of execute method, of class MultiTrain.
     */
    @Test
    public void testExecute() throws Exception {
        System.out.println("execute");
        /*File f = new File(new File(new File(".."),"datasets"), "wwim");
        if (f.exists()) {
            String name = "train";
            List<EvaluationSet> evaluationSets = new ArrayList<>();
            evaluationSets.add(new EvaluationSet(f));
            double negativeSampling = 5.0;
            File configuration = new File(new File(new File(".."),"configs"), "auto.json");
            ExecuteListener monitor = ExecuteListeners.STDERR;
            DatasetLoader loader = new DefaultDatasetLoader();
            MultiTrain.execute(name, evaluationSets, negativeSampling, configuration, monitor, loader, null);
        }*/
    }

}
