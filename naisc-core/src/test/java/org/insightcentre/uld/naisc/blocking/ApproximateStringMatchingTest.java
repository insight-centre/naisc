package org.insightcentre.uld.naisc.blocking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.blocking.ApproximateStringMatching.PatriciaTrie;
import static org.insightcentre.uld.naisc.blocking.ApproximateStringMatching.editDistance;
import org.insightcentre.uld.naisc.lens.Label;
import org.insightcentre.uld.naisc.util.Pair;
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
        assertEquals(expResult, trie.nearest("aa", 2, 1000));
    }
    
    
    @Test
    public void testNearest2() {
        PatriciaTrie<String> trie = new ApproximateStringMatching.PatriciaTrie<>();
        String[] strings = new String[] {
            "t",
            "toni morrison"
        };
        for(int i = 0 ; i < strings.length; i++) {
            String s = strings[i];
            trie.add(s, s);
        }
        List<String> expResult = new ArrayList<String>() {{
            add("toni morrison");
        }};
        assertEquals(expResult, trie.nearest("toni morrison", 1, 1000));
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
        List<String> nearest = trie.nearest("abc", N, 100);
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
    
    
    @Test
    public void testNearestNgramHard() {
        int N = 8;
        ApproximateStringMatching matching = new ApproximateStringMatching();
        BlockingStrategy strat = matching.makeBlockingStrategy(new HashMap<String, Object>() {{ this.put("metric", "ngrams"); this.put("maxMatches", N); this.put("ngrams", 1); }});
                
        Model left = ModelFactory.createDefaultModel();
        left.add(left.createResource("file:tmp#abc"), left.createProperty(Label.RDFS_LABEL), left.createLiteral("abc"));
        Model right = ModelFactory.createDefaultModel();
        List<Resource> strings = new ArrayList<>();
        for(int i = 0 ; i < 100; i++) {
            String s = randString2();
            //System.err.println(s);
            right.add(right.createResource("file:tmp#" + s), right.createProperty(Label.RDFS_LABEL), right.createLiteral(s));
            strings.add(right.createResource("file:tmp#" + s));
        }
        final List<Pair<Resource,Resource>> results = new ArrayList<>();
        for(Pair<Resource,Resource> p : strat.block(left, right)) {
            results.add(p);
        }
        strings.sort(new Comparator<Resource>() {
            @Override
            public int compare(Resource o1, Resource o2) {
                double score1 = ngramSim(o1);
                double score2 = ngramSim(o2);
                int i = -Double.compare(score1, score2);
                return i != 0 ? i : o1.getURI().compareTo(o2.getURI());
            }
        });
        assertEquals(N, results.size());
        for(int i = 0; i < N; i++) {
            //System.err.println(results.get(i)._2 + " " + strings.get(i));
        }
        for(int i = 0; i < N; i++) {
            final Resource r = strings.get(i);
            assert(results.stream().anyMatch(p -> p._2.equals(r)));
        }
    }
    
    private double ngramSim(Resource r) {
        String s = r.getURI().substring(9);
        double score = 0.0;
        for(int i = 0; i < s.length(); i++) {
            if(s.charAt(i) == 'a' || s.charAt(i) == 'b' || s.charAt(i) == 'c') {
                score += 1.0 / s.length();
            }
        }
        return score;
    }
}