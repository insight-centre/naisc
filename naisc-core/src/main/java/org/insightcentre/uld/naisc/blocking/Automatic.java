package org.insightcentre.uld.naisc.blocking;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.BlockingStrategyFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.NaiscListener.Stage;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.LabelResult;
import org.insightcentre.uld.naisc.analysis.MatchResult;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * The smart, automatic blocking strategy
 * 
 * @author John McCrae
 */
public class Automatic implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> _analysis, NaiscListener listener) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        Analysis analysis = _analysis.get();
        // 1. If OntoLex URIs are used assume that we are matching an OntoLex model
        for(String prop : OntoLex.LEXICAL_ENTRY_URLS) {
            if(analysis.leftClasses.contains(prop) || analysis.rightClasses.contains(prop)) {
                return new OntoLex().makeBlockingStrategy(params, _analysis, listener);
            }
        }
        Object2DoubleMap<String> leftUniqueness = new Object2DoubleOpenHashMap<>();
        Object2DoubleMap<String> rightUniqueness = new Object2DoubleOpenHashMap<>();
        
        // 2. Find a good property on the left
        double bestCoverage = -1;
        String bestLeftProp = "";
        for(LabelResult prop : analysis.leftLabels) {
            if(prop.isLabelLike()) {
                if(bestLeftProp == null || prop.coverage > bestCoverage) {
                    bestLeftProp = prop.uri;
                    bestCoverage = prop.coverage;
                }
            }
            leftUniqueness.put(prop.uri, prop.uniqueness);
        }
        // 3. Find a good property on the right
        bestCoverage = -1;
        String bestRightProp = "";
        for(LabelResult prop : analysis.rightLabels) {
            if(prop.isLabelLike()) {
                if(bestRightProp == null || prop.coverage > bestCoverage) {
                    bestRightProp = prop.uri;
                    bestCoverage = prop.coverage;
                }
            }
            rightUniqueness.put(prop.uri, prop.uniqueness);
        }
        // 4. Find any potential good pre-blocks
        Set<Pair<String,String>> preblocks = new HashSet<>();
        for(MatchResult mr : analysis.matching) {
            if(mr.coversData() 
                    && leftUniqueness.getDouble(mr.leftUri) > 0.1 
                    && rightUniqueness.getDouble(mr.rightUri) > 0.1) {
                preblocks.add(new Pair<>(mr.leftUri,mr.rightUri));
            }
        }
        
        listener.message(Stage.INITIALIZING, NaiscListener.Level.INFO, String.format("Matching using properties %s and %s", 
                bestLeftProp.equals("") ? "<URI>" : bestLeftProp, 
                bestRightProp.equals("") ? "<URI>" : bestRightProp));
        if(!preblocks.isEmpty()) {
            StringBuilder sb = new StringBuilder("Pre-blocking on properties:\n");
            for(Pair<String, String> preblock : preblocks) {
                sb.append(String.format("%s <-> %s (%.4f, %.4f)\n", preblock._1, preblock._2, leftUniqueness.getDouble(preblock._1), rightUniqueness.getDouble(preblock._2)));
            }
            listener.message(Stage.INITIALIZING, NaiscListener.Level.INFO, sb.toString());
        }
        return new AutomaticImpl(preblocks, config.maxMatches, bestLeftProp, bestRightProp, config.ngrams);
    }

    /**
     * Configuration for automatic blocking
     */
    public static class Configuration {

        /**
         * The maximum number of matches
         */
        public int maxMatches = 100;
        /**
         * The size of ngrams to use
         */
        public int ngrams = 3;
    
    }
    
    private static class AutomaticImpl implements BlockingStrategy {
        private final Prelinking preblocking;
        private final int maxMatches, n;
        private final String property, rightProperty;
        

        public AutomaticImpl(Set<Pair<String,String>> preblockProperties, int maxMatches, String property, String rightProperty, int n) {
            this.preblocking = new Prelinking(preblockProperties);
            this.maxMatches = maxMatches;
            this.property = property;
            this.rightProperty = rightProperty;
            this.n = n;
        }
        
        

        @Override
        public Iterable<Pair<Resource, Resource>> block(Dataset left,  Dataset right, NaiscListener log) {
            final Set<Pair<Resource, Resource>> p = preblocking.prelink(left, right, log);
            final ApproximateStringMatching.NgramApproximateStringMatch matcher = 
                    new ApproximateStringMatching.NgramApproximateStringMatch(
                            maxMatches, property, rightProperty, n, 
                            Prelinking.leftPrelinked(p), Prelinking.rightPrelinked(p), true);
            final Iterable<Pair<Resource, Resource>> base = matcher.block(left, right, log);
            return new Iterable<Pair<Resource, Resource>>() {
                @Override
                public Iterator<Pair<Resource, Resource>> iterator() {
                    return new Iterator<Pair<Resource, Resource>>() {
                        Iterator<Pair<Resource, Resource>> iter1 = p.iterator(), iter2 = base.iterator();
                        @Override
                        public boolean hasNext() {
                            return iter1.hasNext() || iter2.hasNext();
                        }

                        @Override
                        public Pair<Resource, Resource> next() {
                            if(iter1.hasNext())
                                return iter1.next();
                            else
                                return iter2.next();
                        }
                    };
                }
            };
        }
        
    }
}
