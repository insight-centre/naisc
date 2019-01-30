package org.insightcentre.uld.naisc.util;

import java.util.Iterator;
import java.util.Random;
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
public class TreeNodeTest {

    public TreeNodeTest() {
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
     * Test of iterator method, of class TreeNode.
     */
    @Test
    public void testIterator() {
        System.out.println("iterator");
        TreeNode<Character> instance = new TreeNode();
        populate(instance);
        char e = '`';
        Iterator<Character> result = instance.iterator();
        while (result.hasNext()) {
            char nextChar = result.next();
            assert (e <= nextChar);
            e = nextChar;
        }
    }

    /**
     * Test of isEmpty method, of class TreeNode.
     */
    @Test
    public void testIsEmpty() {
        System.out.println("isEmpty");
        TreeNode instance = new TreeNode();
        boolean expResult = true;
        boolean result = instance.isEmpty();
        assertEquals(expResult, result);
        instance.add("foo", 0);
        expResult = false;
        result = instance.isEmpty();
        assertEquals(expResult, result);

    }

    private void populate(TreeNode t) {
        populate(t, 0);
    }

    private void populate(TreeNode t, int seed) {
        String data = "abcdefghijklmnopqrstuvwxyz";
        Random r = new Random(seed);
        for (int i = 0; i < 20; i++) {
            int j = r.nextInt(26);
            t.add(data.charAt(j), j);
        }
    }

    /**
     * Test of size method, of class TreeNode.
     */
    @Test
    public void testSize() {
        System.out.println("size");
        TreeNode instance = new TreeNode();
        int expResult = 0;
        int result = instance.size();
        assertEquals(expResult, result);
        populate(instance);
        assertEquals(20, instance.size());
    }

    /**
     * Test of add method, of class TreeNode.
     */
    @Test
    public void testAdd() {
        System.out.println("add");
        Object r = "foo";
        double score = 0.0;
        TreeNode instance = new TreeNode();
        instance.add(r, score);
    }

    /**
     * Test of poll method, of class TreeNode.
     */
    @Test
    public void testPoll() {
        System.out.println("poll");
        TreeNode instance = new TreeNode();
        populate(instance);
        instance.add("0", -1);
        int oldSize = instance.size();
        TreeNode.ScoredQueueItem result = instance.poll();
        assertEquals("0", result.r);
        assertEquals(oldSize - 1, instance.size());
    }

    /**
     * Test of peek method, of class TreeNode.
     */
    @Test
    public void testPeek() {
        System.out.println("peek");
        TreeNode instance = new TreeNode();
        populate(instance);
        instance.add("0", -1);
        int oldSize = instance.size();
        TreeNode.ScoredQueueItem result = instance.peek();
        assertEquals("0", result.r);
        assertEquals(oldSize, instance.size());
    }

    /**
     * Test of trim method, of class TreeNode.
     */
    @Test
    public void testTrim() {
        System.out.println("trim");
        int n = 10;
        TreeNode instance = new TreeNode();
        populate(instance);
        instance = instance.trim(n);
        assertEquals(n, instance.size());
        for (int i = 0; i < 100; i++) {
            instance = new TreeNode();
            populate(instance, i);
            instance = instance.trim(n);
            assertEquals(n, instance.size());

        }
    }

}
