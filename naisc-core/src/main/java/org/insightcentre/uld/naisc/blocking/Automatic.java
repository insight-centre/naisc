package org.insightcentre.uld.naisc.blocking;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.*;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.NaiscListener.Stage;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.LabelResult;
import org.insightcentre.uld.naisc.analysis.MatchResult;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.URI2Label;

/**
 * The smart, automatic blocking strategy
 * 
 * @author John McCrae
 */
public class Automatic implements BlockingStrategyFactory {
    private static final String[] LABEL_PROPS = new String[] {
            "http://www.w3.org/2000/01/rdf-schema#label",
            "http://www.w3.org/2004/02/skos/core#prefLabel",
            "http://www.bbc.co.uk/ontologies/coreconcepts/label",
            "http://lexvo.org/ontology#label",
            "http://dbpedia.org/ontology/name",
            "http://www.w3.org/2000/01/rdf-schema#label",
            "http://xmlns.com/foaf/0.1/nick",
            "http://purl.org/dc/elements/1.1/title",
            "http://purl.org/rss/1.0/title",
            "http://xmlns.com/foaf/0.1/name",
            "http://purl.org/dc/terms/title",
            "http://www.geonames.org/ontology#name",
            "http://xmlns.com/foaf/0.1/nickname",
            "http://swrc.ontoware.org/ontology#name",
            "http://sw.cyc.com/CycAnnotations_v1#label",
            "http://rdf.opiumfield.com/lastfm/spec#title",
            "http://www.proteinontology.info/po.owl#ResidueName",
            "http://www.proteinontology.info/po.owl#Atom",
            "http://www.proteinontology.info/po.owl#Element",
            "http://www.proteinontology.info/po.owl#AtomName",
            "http://www.proteinontology.info/po.owl#ChainName",
            "http://purl.uniprot.org/core/fullName",
            "http://purl.uniprot.org/core/title",
            "http://www.aktors.org/ontology/portal#has-title",
            "http://www.w3.org/2004/02/skos/core#prefLabel",
            "http://www.aktors.org/ontology/portal#name",
            "http://xmlns.com/foaf/0.1/givenName",
            "http://www.w3.org/2000/10/swap/pim/contact#fullName",
            "http://xmlns.com/foaf/0.1/surName",
            "http://swrc.ontoware.org/ontology#title",
            "http://swrc.ontoware.org/ontology#booktitle",
            "http://www.aktors.org/ontology/portal#has-pretty-name",
            "http://purl.uniprot.org/core/orfName",
            "http://purl.uniprot.org/core/name",
            "http://www.daml.org/2003/02/fips55/fips-55-ont#name",
            "http://www.geonames.org/ontology#alternateName",
            "http://purl.uniprot.org/core/locusName",
            "http://www.w3.org/2004/02/skos/core#altLabel",
            "http://creativecommons.org/ns#attributionName",
            "http://www.aktors.org/ontology/portal#family-name",
            "http://www.aktors.org/ontology/portal#full-name" };

    private Object2IntMap<String> labelMap() {
        Object2IntMap<String> labelMap = new Object2IntOpenHashMap<>();
        for(int i = 0; i < LABEL_PROPS.length; i++) {
            labelMap.put(LABEL_PROPS[i], LABEL_PROPS.length - i);
        }
        return labelMap;
    }

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> _analysis, NaiscListener listener) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        Analysis analysis = _analysis.get();
        // 1. If OntoLex URIs are used assume that we are matching an OntoLex model
        for(String prop : OntoLex.LEXICAL_ENTRY_URLS) {
            if(analysis.leftClasses.contains(prop) || analysis.rightClasses.contains(prop)) {
                listener.message(Stage.INITIALIZING, NaiscListener.Level.INFO, "Treating as OntoLex matching task");
                return new OntoLex().makeBlockingStrategy(params, _analysis, listener);
            }
        }
        Object2DoubleMap<String> leftUniqueness = new Object2DoubleOpenHashMap<>();
        Object2DoubleMap<String> rightUniqueness = new Object2DoubleOpenHashMap<>();
        Object2IntMap<String> labelMap = labelMap();

        // 2. Find a good property on the left
        double bestCoverage = -1;
        int bestLabel = 0;
        String bestLeftProp = "";
        for(LabelResult prop : analysis.leftLabels) {
            int label = labelMap.getInt(prop.uri);
            if(prop.isLabelLike() || label > 0) {
                if(bestLeftProp == null ||
                        (prop.coverage > bestCoverage && label >= bestLabel) ||
                        (label > bestLabel && prop.coverage > 0.5)) {
                    bestLeftProp = prop.uri;
                    bestCoverage = prop.coverage;
                    bestLabel = label;
                }
            }
            leftUniqueness.put(prop.uri, prop.uniqueness);
        }
        // 3. Find a good property on the right
        bestCoverage = -1;
        bestLabel = 0;
        String bestRightProp = "";
        for(LabelResult prop : analysis.rightLabels) {
            int label = labelMap.getInt(prop.uri);
            if(prop.isLabelLike() || label > 0) {
                if(bestRightProp == null ||
                        (prop.coverage > bestCoverage && label >= bestLabel) ||
                        (label > bestLabel && prop.coverage > 0.5)) {
                    bestRightProp = prop.uri;
                    bestCoverage = prop.coverage;
                    bestLabel = label;
                }
            }
            rightUniqueness.put(prop.uri, prop.uniqueness);
        }
        // 4. Find any potential good pre-blocks
        Set<Pair<String,String>> preblocks = new HashSet<>();
        for(MatchResult mr : analysis.matching) {
            String leftString = URI2Label.fromURI(mr.leftUri).toLowerCase();
            String rightString = URI2Label.fromURI(mr.rightUri).toLowerCase();
            if(leftString.equals(rightString)
                    && mr.coversData()
                    && leftUniqueness.getDouble(mr.leftUri) > 0.5
                    && rightUniqueness.getDouble(mr.rightUri) > 0.5) {
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
    @ConfigurationClass("The smart, automatic matching strategy that builds on the analysis of the datasets to find potential matches. This setting should be used most of the time")
    public static class Configuration {

        /**
         * The maximum number of matches
         */
        @ConfigurationParameter(description = "The maximum number of candidates to generate per entity")
        public int maxMatches = 100;
        /**
         * The size of ngrams to use
         */
        @ConfigurationParameter(description = "The character n-gram to use in matching", defaultValue = "3")
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
        public Collection<Blocking> block(Dataset left, Dataset right, NaiscListener log) {
            final Set<Pair<Resource, Resource>> p = preblocking.prelink(left, right, log);
            final ApproximateStringMatching.NgramApproximateStringMatch matcher =
                    new ApproximateStringMatching.NgramApproximateStringMatch(
                            maxMatches, property, rightProperty, n,
                            Prelinking.leftPrelinked(p), Prelinking.rightPrelinked(p), true, null);
            final Iterable<Blocking> base = matcher.block(left, right, log);
            return new AbstractCollection<Blocking>() {
                @Override
                public Iterator<Blocking> iterator() {
                    return new Iterator<Blocking>() {
                        Iterator<Pair<Resource, Resource>> iter1 = p.iterator();
                        Iterator<Blocking> iter2 = base.iterator();
                        @Override
                        public boolean hasNext() {
                            return iter1.hasNext() || iter2.hasNext();
                        }

                        @Override
                        public Blocking next() {
                            if(iter1.hasNext()) {
                                Pair<Resource,Resource> p = iter1.next();
                                if(p._1 == null || p._2 == null || p._1.getURI() == null || p._2.getURI() == null) {
                                    throw new RuntimeException("Nulls generated in prelinking");
                                }
                                return new Blocking(p._1, p._2, left.id(), right.id());
                            } else
                                return iter2.next();
                        }
                    };
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }
}
