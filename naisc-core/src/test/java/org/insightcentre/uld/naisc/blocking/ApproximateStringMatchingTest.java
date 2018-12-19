package org.insightcentre.uld.naisc.blocking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.insightcentre.uld.naisc.blocking.ApproximateStringMatching.PatriciaTrie;
import static org.insightcentre.uld.naisc.blocking.ApproximateStringMatching.editDistance;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class ApproximateStringMatchingTest {

    public ApproximateStringMatchingTest() {
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
    
    Random random = new Random(1);
    
    public String randString() {
        char[] c = new char[8];
        for(int j = 0; j < 8; j++) {
            c[j] = (char)(random.nextInt(90) + 32);
        }
        return new String(c);
    }
    
    public String randString2() {
        int n = random.nextInt(7) + 1;
        char[] c = new char[n];
        for(int j = 0; j < n; j++) {
            c[j] = (char)(random.nextInt(5) + 97);
        }
        return new String(c);
    }

    @Test
    /**
     * NB this test fails about 1 time in every 10^15 runs.
     */
    public void testPatriciaTrie() {
        PatriciaTrie<String> trie = new ApproximateStringMatching.PatriciaTrie<>();
        List<String> strings = new ArrayList<>();
        for(int i = 0 ; i < 100; i++) {
            String s = randString();
            strings.add(s);
            trie.add(s, s);
        }
        for(String s : strings) {
            assert(!trie.find(s).isEmpty());
        }
        assert(trie.find(randString()).isEmpty());
    }
    
    @Test
    public void testPatriciaTrieHard() {
        PatriciaTrie<String> trie = new ApproximateStringMatching.PatriciaTrie<>();
        String[] strings = new String[] {
            "abc",
            "abcd",
            "ab",
            "aab",
            "abc"
        };
        for(int i = 0 ; i < strings.length; i++) {
            String s = strings[i];
            trie.add(s, s);
        }
        for(String s : strings) {
            assert(!trie.find(s).isEmpty());
        }
        assert(trie.find(randString()).isEmpty());
        
    }
    
    @Test
    public void testEditDistance() {
        String s = "abracadrabra";
        String t = "abcde";
        int expResult = 8;
        assertEquals(expResult, editDistance(s,t));
    }
    
    @Test
    public void testNearest() {
        PatriciaTrie<String> trie = new ApproximateStringMatching.PatriciaTrie<>();
        String[] strings = new String[] {
            "abc",
            "abcd",
            "ab",
            "aab",
            "abc"
        };
        for(int i = 0 ; i < strings.length; i++) {
            String s = strings[i];
            trie.add(s, s);
        }
        List<String> expResult = new ArrayList<String>() {{
            add("aab");
            add("ab");
        }};
        assertEquals(expResult, trie.nearest("aa", 2));
    }
    
    @Test
    public void testNearestHard() {
        PatriciaTrie<String> trie = new ApproximateStringMatching.PatriciaTrie<>();
        List<String> strings = new ArrayList<>();
        for(int i = 0 ; i < 100; i++) {
            String s = randString2();
            strings.add(s);
            trie.add(s, s);
        }
        int N = 8;
        List<String> nearest = trie.nearest("abc", N);
        strings.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                double score1 = (double)editDistance("abc", o1) / (double)(3 + o1.length());
                double score2 = (double)editDistance("abc", o2) / (double)(3 + o2.length());
                int i = Double.compare(score1, score2);
                return i != 0 ? i : o1.compareTo(o2);
            }
        });
        assertEquals(N, nearest.size());
        for(int i = 0; i < N; i++) {
            assert(nearest.contains(strings.get(i)));
        }
    }
}