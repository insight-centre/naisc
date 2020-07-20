package org.insightcentre.uld.naisc.rescaling;

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
public class PercentileTest {

    public PercentileTest() {
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
     * Test of rescale method, of class Percentile.
     */
    @Test
    public void testRescale() {
        System.out.println("rescale");
        double[] value = new double [] {1,2,3,1,8};
        Percentile instance = new Percentile();
        double[] expResult = new double [] { 0.0, 0.5, 0.75, 0.0, 1.0 };
        double[] result = instance.rescale(value);
        assertArrayEquals(expResult, result, 0.01);
    }

    /**
     * Test of rescale method, of class Percentile.
     */
    @Test
    public void testRescale2() {
        System.out.println("rescale2");
        double[] value = new double [] {1,2,3,1,8,8};
        Percentile instance = new Percentile();
        double[] expResult = new double [] { 0.0, 0.5, 0.75, 0.0, 1.0,1.0 };
        double[] result = instance.rescale(value);
        assertArrayEquals(expResult, result, 0.01);
    }

    @Test
    public void testRescale3() {
        System.out.println("rescale2");
        double[] value = new double [] {0,1,1e-13};
        Percentile instance = new Percentile();
        double[] expResult = new double [] { 0.0, 1.0, 0.0 };
        double[] result = instance.rescale(value);
        assertArrayEquals(expResult, result, 0.01);
    }

    @Test
    public void testRescale4() {
        System.out.println("rescale2");
        double[] value = new double [] {0,1,1e-13,0.5, 0.5+1e-13,0.5-1e-13};
        Percentile instance = new Percentile();
        double[] expResult = new double [] { 0.0, 1.0, 0.0, 0.666, 0.666, 0.666 };
        double[] result = instance.rescale(value);
        assertArrayEquals(expResult, result, 0.01);
    }}