package org.insightcentre.uld.naisc.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
public class SimpleCacheTest {

    public SimpleCacheTest() {
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
     * Test of get method, of class SimpleCache.
     */
    @Test
    public void testThreadSafety() throws Exception {
        System.out.println("get");
        ExecutorService executor = Executors.newFixedThreadPool(20);
        final SimpleCache<String, String> instance = new SimpleCache<>(5);
        for(int i = 0; i < 1000; i++) {
            final int j = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    final String s1 = String.format("x%d", j % 50);
                    final String s = instance.get(s1, t -> String.format("%sy", t));
                    assertEquals(String.format("x%dy", j % 50), s);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(0, TimeUnit.MINUTES);
    }


}