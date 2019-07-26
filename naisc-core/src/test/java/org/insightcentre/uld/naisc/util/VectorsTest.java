package org.insightcentre.uld.naisc.util;

import org.insightcentre.uld.naisc.feature.embeddings.DenseVector;
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
public class VectorsTest {

    public VectorsTest() {
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
     * Test of cosine method, of class Vectors.
     */
    @Test
    public void testCosine() {
        System.out.println("cosine");
        Vector _v1 = new DenseVector(new double[] { 0.0, 0.0, 0.0 });
        Vector _v2 = new DenseVector(new double[] { 1.0, 2.0, 3.0 });
        double expResult = 0.0;
        double result = Vectors.cosine(_v1, _v2);
        assertEquals(expResult, result, 0.0);
    }

}