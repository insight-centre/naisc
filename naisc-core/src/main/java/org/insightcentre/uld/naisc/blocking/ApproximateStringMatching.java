package org.insightcentre.uld.naisc.blocking;

import org.insightcentre.uld.naisc.util.TreeNode;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.BlockingStrategyFactory;
import static org.insightcentre.uld.naisc.lens.Label.RDFS_LABEL;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Find the terms that match by the minimum of Levenshtein Edit Distance.
 *
 * @author John McCrae
 */
public class ApproximateStringMatching implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if (config.maxMatches < 1) {
            throw new ConfigurationException("Max matches must be at least one");
        }
        if (config.queueMax < 1) {
            config.queueMax = config.maxMatches * 20;
        }
        if (config.property == null || config.property.equals("")) {
            throw new ConfigurationException("Property must be set");
        }

        return new ApproximateStringMatchingImpl(config.maxMatches, config.property, config.rightProperty, config.queueMax);
    }

    /**
     * Configuration for approximate string matching
     */
    public static class Configuration {

        /**
         * The maximum number of matches
         */
        public int maxMatches;
        /**
         * The labeling property
         */
        public String property = RDFS_LABEL;
        /**
         * The property for the right ontology (if different)
         */
        public String rightProperty = null;
        /**
         * The maximum size of the queue (sets the default queue size, 0 for no limit)
         */
        public int queueMax = maxMatches * 20;
    }

    private static class ApproximateStringMatchingImpl implements BlockingStrategy {

        private final int maxMatches;
        private final String property;
        private final String rightProperty;
        private final int queueMax;

        public ApproximateStringMatchingImpl(int maxMatches, String property, String rightProperty, int queueMax) {
            this.maxMatches = maxMatches;
            this.property = property;
            this.rightProperty = rightProperty;
            this.queueMax = queueMax;
        }

        @Override
        @SuppressWarnings("Convert2Lambda")
        public Iterable<Pair<Resource, Resource>> block(Model left, Model right) {
            final List<Resource> lefts = new ArrayList<>();
            Property leftProp = left.createProperty(property);
            ResIterator leftIter = left.listSubjectsWithProperty(leftProp);
            while (leftIter.hasNext()) {
                Resource r = leftIter.next();
                if (r.isURIResource()) {
                    lefts.add(r);
                }
            }

            final List<Resource> rights = new ArrayList<>();
            Property rightProp = right.createProperty(rightProperty != null && !rightProperty.equals("") ? rightProperty : property);
            ResIterator rightIter = right.listSubjectsWithProperty(rightProp);
            while (rightIter.hasNext()) {
                Resource r = rightIter.next();
                if (r.isURIResource()) {
                    rights.add(r);
                }
            }

            final PatriciaTrie<Resource> trie = new PatriciaTrie<>();
            for (Resource r : rights) {
                NodeIterator iter = right.listObjectsOfProperty(r, rightProp);
                while (iter.hasNext()) {
                    RDFNode n = iter.next();
                    if (n.isLiteral()) {
                        trie.add(n.asLiteral().getLexicalForm(), r);
                    }
                }
            }
            final List<Pair<Resource, String>> labels = new ArrayList<>();
            for (Resource r : lefts) {
                NodeIterator iter = left.listObjectsOfProperty(r, leftProp);
                while (iter.hasNext()) {
                    RDFNode n = iter.next();
                    if (n.isLiteral()) {
                        labels.add(new Pair(r, n.asLiteral().getLexicalForm()));
                    }
                }
            }

            return new Iterable<Pair<Resource, Resource>>() {
                @Override
                public Iterator<Pair<Resource, Resource>> iterator() {
                    return labels.stream().flatMap(pair -> {
                        return trie.nearest(pair._2, maxMatches, queueMax).stream().
                                map(x -> new Pair<Resource, Resource>(pair._1, x));
                    }).iterator();
                }

            };
        }

    }

    public static int editDistance(String s, String t) {
        String left = s.length() < t.length() ? s : t;
        String right = s.length() < t.length() ? t : s;
        int[] last = new int[left.length() + 1];
        int[] next = new int[left.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            last[i] = i;
        }
        for (int j = 1; j <= right.length(); j++) {
            Arrays.fill(next, 0);
            next[0] = last[0] + 1;
            for (int i = 1; i <= left.length(); i++) {
                next[i] = Math.min(
                        Math.min(
                                last[i] + 1,
                                next[i - 1] + 1
                        ),
                        last[i - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1)
                );
            }
            System.arraycopy(next, 0, last, 0, last.length);
        }
        return next[left.length()];
    }

    /**
     * Returns the lower bound of the edit distance between left and strings
     * starting with right
     *
     * @param left
     * @param right
     * @return
     */
    private static int editDistanceLowerBound(String left, String right) {
        int[] last = new int[left.length() + 1];
        int[] next = new int[left.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            last[i] = i;
        }
        for (int j = 1; j <= right.length(); j++) {
            Arrays.fill(next, 0);
            next[0] = last[0] + 1;
            for (int i = 1; i <= left.length(); i++) {
                next[i] = Math.min(
                        Math.min(
                                last[i] + 1,
                                next[i - 1] + 1
                        ),
                        last[i - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1)
                );
            }
            System.arraycopy(next, 0, last, 0, last.length);
        }
        int min = Math.max(left.length(), right.length());
        for (int j : next) {
            min = Math.min(min, j);
        }
        return min;
    }

    public static class PatriciaTrie<R> {

        private TrieNode<R> root = new TrieNode<>();

        public void add(String s, R r) {
            if (s.length() > 0) {
                addToNode(s, r, root);
            }
        }

        private static <R> ArrayList<R> singleton(R r) {
            ArrayList<R> rs = new ArrayList<>();
            rs.add(r);
            return rs;
        }

        private void addToNode(String s, R r, TrieNode<R> node) {
            for (TrieLink<R> child : node.children) {
                int i = longestInitialSubsequence(s, child.link);
                if (i > 0 && i < child.link.length()) {
                    // This is a fork
                    node.children.remove(child);
                    if (i < s.length()) {
                        String common = s.substring(0, i);
                        List<TrieLink<R>> x = new ArrayList<>();
                        x.add(new TrieLink(s.substring(i),
                                new TrieNode(new ArrayList<>(), singleton(r))));
                        x.add(new TrieLink(child.link.substring(i), child.node));

                        node.children.add(new TrieLink(common, new TrieNode(x, new ArrayList<>())));
                        return;

                    } else {
                        String common = s;
                        List<TrieLink<R>> x = new ArrayList<>();
                        x.add(new TrieLink(child.link.substring(i), child.node));

                        node.children.add(new TrieLink(common, new TrieNode(x, singleton(r))));
                        return;
                    }
                } else if (i == child.link.length()) {
                    if (i == s.length()) {
                        // Exactly the same string
                        child.node.values.add(r);
                        return;
                    } else {
                        addToNode(s.substring(i), r, child.node);
                        return;
                    }
                }
            }
            // Failed to find an overlapping child
            node.children.add(new TrieLink(s, new TrieNode(new ArrayList<>(), singleton(r))));

        }

        public List<R> find(String s) {
            return _find(s, root);
        }

        private List<R> _find(String s, TrieNode<R> node) {

            for (TrieLink<R> child : node.children) {
                int i = longestInitialSubsequence(s, child.link);
                if (i > 0 && i < child.link.length()) {
                    return Collections.EMPTY_LIST;
                } else if (i == child.link.length()) {
                    if (i == s.length()) {
                        return child.node.values;
                    } else {
                        return _find(s.substring(i), child.node);
                    }
                }
            }
            return Collections.EMPTY_LIST;
        }

        public List<R> nearest(String s, int n, int queueMax) {
            ScoredQueue<TrieLink<R>> queue = new ScoredQueue<>();
            ScoredQueue<R> beam = new ScoredQueue<>();
            for (TrieLink<R> link : root.children) {
                double lowerBound = lowerBound(s, link.link);
                queue.add(link, lowerBound);
            }
            while (!queue.isEmpty()) {
                if (queue.minimum() > beam.maximum() && beam.size() >= n) {
                    break;
                }
                TrieLink<R> link = queue.poll();
                for (TrieLink<R> link2 : link.node.children) {
                    String key = link.link + link2.link;
                    double lowerBound = lowerBound(s, key);
                    if (lowerBound < beam.maximum() || beam.size() < n) {
                        queue.add(new TrieLink<>(key, link2.node), lowerBound);
                    }
                }
                if(queue.size() > queueMax) {
                    queue.trim(queueMax);
                }
                double score = (double) editDistance(s, link.link) / (double) (s.length() + link.link.length());
                for (R r : link.node.values) {
                    if (score < beam.maximum() || beam.size() < n) {
                        beam.add(r, score);
                    }
                }
                beam.trim(n);
            }
            return new ArrayList<>(beam);
        }

        private static double lowerBound(String s, String t) {
            if (s.length() <= t.length()) {
                return (double) editDistance(s, t) / (double) (s.length() + t.length());
            } else {
                // T is not as long as S yet, so we assume that all the missing characters exactly match S
                int diff = s.length() - t.length();
                return (double) editDistanceLowerBound(s, t) / (double) (s.length() + t.length() + diff);
            }
        }

        private int longestInitialSubsequence(String s, String t) {
            for (int i = 0; i < s.length() && i < t.length(); i++) {
                if (s.charAt(i) != t.charAt(i)) {
                    return i;
                }
            }
            return Math.min(s.length(), t.length());
        }
    }

    private static class TrieNode<R> {

        public final List<TrieLink<R>> children;
        private final List<R> values;

        public TrieNode() {
            this.children = new ArrayList<>();
            this.values = new ArrayList<>();
        }

        public TrieNode(List<TrieLink<R>> children, List<R> values) {
            this.children = children;
            this.values = values;
            assert (this.values != null);
            for (TrieLink<R> c : children) {
                assert (c.link.length() > 0);
            }
        }
    }

    private static class TrieLink<R> {

        public final String link;
        public final TrieNode<R> node;

        public TrieLink(String link, TrieNode<R> node) {
            this.link = link;
            this.node = node;
        }
    }

    
    private static class ScoredQueue<R> extends AbstractCollection<R> {

        private TreeNode<R> queue;
        private double maximum = Double.NEGATIVE_INFINITY;

        public ScoredQueue() {
            this.queue = new TreeNode<>();
        }

        @Override
        public Iterator<R> iterator() {
            return queue.iterator();
        }
        
        @Override
        public int size() {
            return queue.size();
        }

        public void add(R r, double score) {
            maximum = Math.max(maximum, score);
            queue.add(r, score);
        }

        public R poll() {
            TreeNode.ScoredQueueItem<R> r = queue.poll();
            if (this.isEmpty()) {
                maximum = Double.NEGATIVE_INFINITY;
            }
            return r.r;
        }

        public void trim(int n) {
            if (queue.size() > n) {
                queue = queue.trim(n);
            }
        }

        public double maximum() {
            if (this.isEmpty()) {
                return Double.POSITIVE_INFINITY;
            } else {
                return maximum;
            }
        }

        public double minimum() {
            if (this.isEmpty()) {
                return Double.POSITIVE_INFINITY;
            } else {
                return queue.peek().score;
            }
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

    }
    
    
}
