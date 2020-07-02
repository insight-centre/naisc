package org.insightcentre.uld.naisc.blocking;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.TreeNode;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

import org.insightcentre.uld.naisc.NaiscListener.Stage;
import org.insightcentre.uld.naisc.analysis.Analysis;
import static org.insightcentre.uld.naisc.lens.Label.RDFS_LABEL;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.URI2Label;

/**
 * Find the terms that match by the minimum of Levenshtein Edit Distance.
 *
 * @author John McCrae
 */
public class ApproximateStringMatching implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> analysis, NaiscListener listener) {
        Configuration config = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if (config.maxMatches < 1) {
            throw new ConfigurationException("Max matches must be at least one (or is not set)");
        }
        if (config.metric == null) {
            config.metric = StringMetric.ngrams;
        }
        if (config.metric == StringMetric.levenshtein && config.queueMax < 1) {
            config.queueMax = config.maxMatches * 20;
        }
        if (config.property == null || config.property.equals("")) {
            throw new ConfigurationException("Property must be set");
        }
        if (config.rightProperty == null || config.rightProperty.equals("")) {
            config.rightProperty = config.property;
        }
        if (config.metric == StringMetric.ngrams && config.ngrams < 1) {
            config.ngrams = 3;
        }
        switch (config.metric) {
            case levenshtein:
                return new LevenshteinApproximateStringMatch(config.maxMatches, config.property, config.rightProperty, config.queueMax, config.lowercase);
            case ngrams:
                return new NgramApproximateStringMatch(config.maxMatches, config.property, config.rightProperty, config.ngrams, config.lowercase, config.type);
            default:
                throw new RuntimeException("Unreachable");
        }
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
         * The maximum size of the queue (sets the default queue size, 0 for no
         * limit, only for Levenshtein)
         */
        public int queueMax = maxMatches * 20;
        /**
         * The metric to use
         */
        public StringMetric metric = StringMetric.ngrams;
        /**
         * The size of ngrams to use
         */
        public int ngrams = 3;
        /**
         * Lowercase all strings
         */
        public boolean lowercase = true;
        /**
         * Type of the element. If set all matched elements are of rdf:type with this URI
         */
        public String type = null;
    }

    public enum StringMetric {
        levenshtein,
        ngrams
    }

    static class NgramApproximateStringMatch implements BlockingStrategy {

        private final int maxMatches;
        private final String property;
        private final String rightProperty;
        private final int n;
        private final Set<Resource> leftPreBlocks, rightPreBlocks;
        private final boolean lowercase;
        private final String type;

        public NgramApproximateStringMatch(int maxMatches, String property, String rightProperty, int n, boolean lowercase, String type) {
            this.maxMatches = maxMatches;
            this.property = property;
            this.rightProperty = rightProperty;
            this.n = n;
            this.leftPreBlocks = Collections.EMPTY_SET;
            this.rightPreBlocks = Collections.EMPTY_SET;
            this.lowercase = lowercase;
            this.type = type;
        }

        public NgramApproximateStringMatch(int maxMatches, String property, String rightProperty, int n, Set<Resource> leftPreBlocks, Set<Resource> rightPreBlocks, boolean lowercase, String type) {
            this.maxMatches = maxMatches;
            this.property = property;
            this.rightProperty = rightProperty;
            this.n = n;
            this.leftPreBlocks = leftPreBlocks;
            this.rightPreBlocks = rightPreBlocks;
            this.lowercase = lowercase;
            this.type = type;
        }

        private boolean hasType(Dataset ds, Resource r, Resource type) {
            return ds.listStatements(r, RDF.type, type).hasNext();
        }

        @Override
        public Collection<Blocking> block(Dataset left, Dataset right, NaiscListener log) {
            final Resource leftType = type == null ? null : left.createResource(type);
            final Resource rightType = type == null ? null : right.createResource(type);
            final List<Resource> lefts = new ArrayList<>();
            final ResIterator leftIter;
            if (property.equals("")) {
                leftIter = left.listSubjects();
            } else {
                leftIter = left.listSubjectsWithProperty(left.createProperty(property));
            }
            while (leftIter.hasNext()) {
                Resource r = leftIter.next();
                if (r.isURIResource() && (type == null || hasType(left, r, leftType))) {
                    lefts.add(r);
                }
            }
            int nLeft = lefts.size();
            lefts.removeAll(leftPreBlocks);
            log.message(Stage.BLOCKING, NaiscListener.Level.INFO, String.format("%d entities in left dataset (%d preblocked)", lefts.size(), nLeft - lefts.size()));

            final List<Resource> rights = new ArrayList<>();
            final ResIterator rightIter;
            if (rightProperty.equals("")) {
                rightIter = right.listSubjects();
            } else {
                Property rightProp = right.createProperty(rightProperty);
                rightIter = right.listSubjectsWithProperty(rightProp);
            }
            while (rightIter.hasNext()) {
                Resource r = rightIter.next();
                if (r.isURIResource() && (type == null || hasType(right, r, rightType))) {
                    rights.add(r);
                }
            }
            int nRight = rights.size();
            rights.removeAll(rightPreBlocks);
            log.message(Stage.BLOCKING, NaiscListener.Level.INFO, String.format("%d entities in right dataset (%d preblocked)", rights.size(), nRight - rights.size()));

            final Map<String, Map<Resource, FreqLen>> ngrams = new HashMap<>();
            for (Resource r : rights) {
                if (rightProperty.equals("")) {
                    String s = URI2Label.fromURI(r.getURI());
                            if(lowercase) s = s.toLowerCase();
                    extractNgram(s, ngrams, r);
                } else {
                    Property rightProp = right.createProperty(rightProperty);
                    NodeIterator iter = right.listObjectsOfProperty(r, rightProp);
                    while (iter.hasNext()) {
                        RDFNode node = iter.next();
                        if (node.isLiteral()) {
                            String s = node.asLiteral().getLexicalForm();
                            if(lowercase) s = s.toLowerCase();
                            extractNgram(s, ngrams, r);
                        }
                    }
                }
            }
            // Heuristically remove any very common n-grams
            int ngramsSize = ngrams.size();
            Iterator<Map.Entry<String, Map<Resource, FreqLen>>> ngramsIter = ngrams.entrySet().iterator();
            while(ngramsIter.hasNext()) {
                Map.Entry<String, Map<Resource, FreqLen>> e = ngramsIter.next();
                if(e.getValue().size() > 1000)
                    ngramsIter.remove();
            }
            if((double)ngrams.size() / ngramsSize < 0.9)
                log.message(Stage.BLOCKING, NaiscListener.Level.WARNING, "N-Gram in blocking leads to poor matching, consider changing the value of the parameter ngrams (current value=" + ngramsSize + ")");
            final List<Pair<Resource, List<String>>> labels = new ArrayList<>();
            for (Resource r : lefts) {
                if (property.equals("")) {
                    String s = URI2Label.fromURI(r.getURI());
                            if(lowercase) s = s.toLowerCase();
                    labels.add(new Pair(r, Arrays.asList(s)));
                } else {
                    NodeIterator iter = left.listObjectsOfProperty(r, left.createProperty(property));
                    List<String> l = new ArrayList<>();
                    while (iter.hasNext()) {
                        RDFNode n = iter.next();
                        if (n.isLiteral()) {
                            String s = n.asLiteral().getLexicalForm();
                            if(lowercase) s = s.toLowerCase();
                            l.add(s);
                        }
                    }
                    labels.add(new Pair(r, l));
                }
            }

            return new AbstractCollection<Blocking>() {
                @Override
                public Iterator<Blocking> iterator() {
                    return labels.stream().flatMap(pair -> {
                        return nearest(pair._2, ngrams).stream().
                                map(x -> new Blocking(pair._1, x, left.id(), right.id()));
                    }).iterator();
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private void extractNgram(String s, final Map<String, Map<Resource, FreqLen>> ngrams, Resource r) {
            for (int i = 0; i < s.length() - this.n + 1; i++) {
                String ng = s.substring(i, i + this.n);
                final Map<Resource, FreqLen> ngMap;
                if (!ngrams.containsKey(ng)) {
                    ngrams.put(ng, ngMap = new HashMap<>());
                } else {
                    ngMap = ngrams.get(ng);
                }
                ngMap.put(r, ngMap.get(r) == null ? new FreqLen(1.0, s.length()) : new FreqLen(ngMap.get(r).freq + 1, s.length()));
            }
        }

        private List<Resource> nearest(List<String> labels, Map<String, Map<Resource, FreqLen>> ngrams) {
            final Object2DoubleMap<Resource> freqsFinal = new Object2DoubleOpenHashMap<>();
            final Object2IntMap<String> reps = new Object2IntOpenHashMap<>();
            for (String r : labels) {
                for (int i = 0; i < Math.min(100,r.length()) - n + 1; i++) {
                    String ng = r.substring(i, i + n);
                    Map<Resource, FreqLen> ngs = ngrams.get(ng);
                    if (ngs != null) {
                        for (Map.Entry<Resource, FreqLen> e : ngs.entrySet()) {
                            if(reps.getInt(ng) < e.getValue().freq)
                                freqsFinal.put(e.getKey(), freqsFinal.getDouble(e.getKey()) + 1.0 / (e.getValue().len + r.length()));
                        }
                            reps.put(ng, reps.getInt(ng) + 1);
                    }
                }
            }
            List<Resource> resList = new ArrayList<>(freqsFinal.keySet());
            Collections.sort(resList, new Comparator<Resource>() {
                @Override
                public int compare(Resource o1, Resource o2) {
                    double f1 = freqsFinal.getDouble(o1);
                    double f2 = freqsFinal.getDouble(o2);
                    if (f1 > f2) {
                        return -1;
                    } else if (f1 < f2) {
                        return +1;
                    } else {
                        return o1.getURI().compareTo(o2.getURI());
                    }
                }
            });
            if (resList.size() > maxMatches) {
                return resList.subList(0, maxMatches);
            } else {
                return resList;
            }
        }

        @Override
        public int estimateSize(Dataset left, Dataset right) {
            Iterator<Resource> i1 = left.listSubjects();
            Iterator<Resource> i2 = right.listSubjects();
            int n = 0;
            while (i1.hasNext()) {
                n++;
                i1.next();
            }
            int m = 0;
            while (i2.hasNext()) {
                m++;
                i2.next();
            }
            return n * Math.min(m, maxMatches);
        }

    }

    static class LevenshteinApproximateStringMatch implements BlockingStrategy {

        private final int maxMatches;
        private final String property;
        private final String rightProperty;
        private final int queueMax;
        private final boolean lowercase;

        public LevenshteinApproximateStringMatch(int maxMatches, String property, String rightProperty, int queueMax, boolean lowercase) {
            this.maxMatches = maxMatches;
            this.property = property;
            this.rightProperty = rightProperty;
            this.queueMax = queueMax;
            this.lowercase = lowercase;
        }

        @Override
        @SuppressWarnings("Convert2Lambda")
        public Collection<Blocking> block(Dataset left, Dataset right, NaiscListener log) {
            final List<Resource> lefts = new ArrayList<>();
            Property leftProp = left.createProperty(property);
            ResIterator leftIter = left.listSubjectsWithProperty(leftProp);
            while (leftIter.hasNext()) {
                Resource r = leftIter.next();
                if (r.isURIResource()) {
                    lefts.add(r);
                }
            }
            if (lefts.isEmpty()) {
                log.message(NaiscListener.Stage.BLOCKING, NaiscListener.Level.CRITICAL, "No URIs in the left dataset have the property: " + property);
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
            if (rights.isEmpty()) {
                log.message(NaiscListener.Stage.BLOCKING, NaiscListener.Level.CRITICAL, "No URIs in the right dataset have the property: " + property);
            }

            final PatriciaTrie<Resource> trie = new PatriciaTrie<>();
            for (Resource r : rights) {
                NodeIterator iter = right.listObjectsOfProperty(r, rightProp);
                while (iter.hasNext()) {
                    RDFNode n = iter.next();
                    if (n.isLiteral()) {
                        trie.add(lowercase ? n.asLiteral().getLexicalForm().toLowerCase() : n.asLiteral().getLexicalForm(), r);
                    } else {
                        log.message(NaiscListener.Stage.BLOCKING, NaiscListener.Level.WARNING, r + " had a non-literal value for property " + property);
                    }
                }
            }
            final List<Pair<Resource, String>> labels = new ArrayList<>();
            for (Resource r : lefts) {
                NodeIterator iter = left.listObjectsOfProperty(r, leftProp);
                while (iter.hasNext()) {
                    RDFNode n = iter.next();
                    if (n.isLiteral()) {
                        labels.add(new Pair(r, lowercase ? n.asLiteral().getLexicalForm().toLowerCase() : n.asLiteral().getLexicalForm()));
                    } else {
                        log.message(NaiscListener.Stage.BLOCKING, NaiscListener.Level.WARNING, r + " had a non-literal value for property " + property);
                    }
                }
            }

            // TODO: Should only produce pairs per entry not label as NGram Approximate matcher
            return new AbstractCollection<Blocking>() {
                @Override
                public Iterator<Blocking> iterator() {
                    return labels.stream().flatMap(pair -> {
                        return trie.nearest(pair._2, maxMatches, queueMax).stream().
                                map(x -> new Blocking(pair._1, x, left.id(), right.id()));
                    }).iterator();
                }
                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }

            };
        }

        @Override
        public int estimateSize(Dataset left, Dataset right) {
            Iterator<Resource> i1 = left.listSubjects();
            Iterator<Resource> i2 = right.listSubjects();
            int n = 0;
            while (i1.hasNext()) {
                n++;
                i1.next();
            }
            int m = 0;
            while (i2.hasNext()) {
                m++;
                i2.next();
            }
            return n * Math.min(m, maxMatches);
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
                if (queue.size() > queueMax) {
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

    private static class FreqLen {
        public final double freq, len;

        public FreqLen(double freq, double len) {
            this.freq = freq;
            this.len = len;
        }

        @Override
        public String toString() {
            return "FreqLen{" + "freq=" + freq + ", len=" + len + '}';
        }

        
    }
}
