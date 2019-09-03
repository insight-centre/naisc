package org.insightcentre.uld.naisc.util;

import java.util.Arrays;
import java.util.HashSet;
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
public class EdmondsChuLiuTest {

    public EdmondsChuLiuTest() {
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
     * Test of getMinBranching method, of class EdmondsChuLiu.
     */
    @Test
    public void testGetMinBranching() {
        System.out.println("getMinBranching");
        EdmondsChuLiu.Node node1 = new EdmondsChuLiu.Node<>(1);
        EdmondsChuLiu.Node node2 = new EdmondsChuLiu.Node<>(2);
        EdmondsChuLiu.Node node3 = new EdmondsChuLiu.Node<>(3);
        EdmondsChuLiu.Node node4 = new EdmondsChuLiu.Node<>(4);
        EdmondsChuLiu.AdjacencyList list = new EdmondsChuLiu.AdjacencyList();
        list.addEdge(node1, node2, 1.0, null);
        list.addEdge(node1, node3, 2.0, null);
        list.addEdge(node1, node4, 3.0, null);
        list.addEdge(node2, node4, 1.0, null);
        
        EdmondsChuLiu instance = new EdmondsChuLiu();
        
        EdmondsChuLiu.AdjacencyList result = instance.getMinBranching(node1, list);
        assertEquals(result.getAdjNodes(node1), new HashSet<>(Arrays.asList(node2,node3)));
        assertEquals(result.getAdjNodes(node2), new HashSet<>(Arrays.asList(node4)));
        assertEquals(result.getAdjNodes(node3), new HashSet<>(Arrays.asList()));
        assertEquals(result.getAdjNodes(node4), new HashSet<>(Arrays.asList()));
    }

}