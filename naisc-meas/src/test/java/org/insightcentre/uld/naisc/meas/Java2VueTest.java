package org.insightcentre.uld.naisc.meas;

import org.insightcentre.uld.naisc.main.Configuration;
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
public class Java2VueTest {

    public Java2VueTest() {
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
     * Test of java2vue method, of class Java2Vue.
     */
    @Test
    public void testJava2vue() {
        System.out.println("java2vue");
        Class configClass = Configuration.class;
        String result = Java2Vue.java2vue(configClass);
        assert(result.contains("The method to match"));
    }

}