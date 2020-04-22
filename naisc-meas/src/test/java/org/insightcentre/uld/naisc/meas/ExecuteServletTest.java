package org.insightcentre.uld.naisc.meas;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.meas.execution.Execution;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class ExecuteServletTest {

    public ExecuteServletTest() {
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

    @Test
    public void testAddBlock() throws Exception {
        final Execution execution = new Execution(null);
        final ExecutorService threadPool = Executors.newFixedThreadPool(10);
        final Model model = ModelFactory.createDefaultModel();
        for(int i = 0; i < 1000; i++) {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    execution.addBlock(new URIRes("foo", "left"), new URIRes("bar", "right"));
                }
            });
        }
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.MINUTES);
    }

}