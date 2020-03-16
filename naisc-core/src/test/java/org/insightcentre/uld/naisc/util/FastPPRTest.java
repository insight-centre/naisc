package org.insightcentre.uld.naisc.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
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
public class FastPPRTest {

    public FastPPRTest() {
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
     * Test of estimatePPR method, of class FastPPR.
     */
    @Test
    public void testEstimatePPR() {
        System.out.println("estimatePPR");
        FastPPR.DirectedGraph graph = new FastPPR.DirectedGraph();
        graph.addNode();
        graph.addNode();
        graph.addNode();
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        int startId = 0;
        int targetId = 2;
        FastPPR.FastPPRConfiguration config = new FastPPR.FastPPRConfiguration();
        float expResult = 0.128F;
        float result = FastPPR.estimatePPR(graph, startId, targetId, config);
        assertEquals(expResult, result, 0.01);

        graph.addEdge(0, 2);
        float result2 = FastPPR.estimatePPR(graph, startId, targetId, config);
        assert (result2 > result);

    }

    @Test
    public void testEstimatePPR2() throws IOException {
        FastPPR.FastPPRConfiguration config = new FastPPR.FastPPRConfiguration();
        config.pprSignificanceThreshold = 0.03f;
        float approximationRatio = 1.4f;
        FastPPR.DirectedGraph graph = testGraph();
        BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/test_graph_true_pprs.txt"));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] pieces = line.split("\t");
            int startId = Integer.parseInt(pieces[0]);
            int targetId = Integer.parseInt(pieces[1]);
            float truePPR = Float.parseFloat(pieces[2]);
            for (boolean balanced : Arrays.asList(true, false)) {
                float estimate = FastPPR.estimatePPR(graph, startId, targetId, config, balanced);
                assert (estimate > truePPR / approximationRatio);
                assert (estimate < truePPR * approximationRatio);
            }
        }
    }

    private FastPPR.DirectedGraph testGraph() {
        FastPPR.DirectedGraph graph = new FastPPR.DirectedGraph();
        graph.addNode();
        graph.addNode();
        graph.addNode();
        graph.addNode();
        graph.addNode();
        graph.addNode();
        graph.addNode();
        graph.addNode();
        graph.addNode();
        graph.addNode();
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 0);
        graph.addEdge(1, 2);
        graph.addEdge(2, 0);
        graph.addEdge(2, 1);
        graph.addEdge(2, 3);
        graph.addEdge(2, 9);
        graph.addEdge(3, 0);
        graph.addEdge(9, 0);
        graph.addEdge(4, 2);
        graph.addEdge(4, 3);
        graph.addEdge(5, 3);
        graph.addEdge(5, 4);
        graph.addEdge(6, 1);
        graph.addEdge(6, 3);
        graph.addEdge(7, 0);
        graph.addEdge(7, 5);
        graph.addEdge(8, 2);
        graph.addEdge(8, 3);
        return graph;
    }
}
