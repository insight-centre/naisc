
package org.insightcentre.uld.naisc.util;

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
public class PrettyGoodTokenizerTest {

    public PrettyGoodTokenizerTest() {
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
     * Test of tokenize method, of class PrettyGoodTokenizer.
     */
    @Test
    public void testTokenize() {
        System.out.println("tokenize");
        String s = "This a (test) that uses commas, and punctuation.";
        String[] expResult = new String[] { "This","a","(","test",")","that","uses","commas",",","and","punctuation","."};
        String[] result = PrettyGoodTokenizer.tokenize(s);
        assertArrayEquals(expResult, result);
    }

}