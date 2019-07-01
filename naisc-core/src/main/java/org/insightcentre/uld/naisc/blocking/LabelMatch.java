package org.insightcentre.uld.naisc.blocking;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.BlockingStrategyFactory;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.PrettyGoodTokenizer;

/**
 * Only match entities that have identical or similar labels.
 *
 * @author John McCrae
 */
public class LabelMatch implements BlockingStrategyFactory {

    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params) {
        Configuration config = mapper.convertValue(params, Configuration.class);
        if (config.property == null) {
            throw new ConfigurationException("Label match requires a labelling property");
        }
        return new LabelMatchImpl(config.property,
                config.rightProperty != null ? config.rightProperty : config.property,
                config.language != null ? Language.get(config.language) : null,
                config.mode == null ? Mode.strict : config.mode,
                config.lowercase);
    }

    /**
     * Configuration for the label match blocking strategy.
     */
    public static class Configuration {

        /**
         * The label property to match on
         */
        @ConfigurationParameter(description="The label property to match on", defaultValue = "\"http://www.w3.org/2000/01/rdf-schema#label\"")
        public String property;
        /**
         * The label property in the right datasets. Null uses the
         * <code>property</code> value for both left and right datasets.
         */
        @ConfigurationParameter(description="The label property in the right datasets (if different from left)", defaultValue="\"\"")
        public String rightProperty;
        /**
         * The language to match on, as an ISO 639 code. Null matches without
         * regard for language.
         */
        @ConfigurationParameter(description="The language to match on, as an ISO 639 code", defaultValue="\"en\"")
        public String language;
        /**
         * The mode. One of strict (exact match), semiLenient (substring match),
         * lenient (word match)
         */
        @ConfigurationParameter(description = "The mode to match; strict for exact matching, or lenient for partial", defaultValue="\"strict\"")
        public Mode mode = Mode.strict;
        /**
         * Whether to lowercase labels before matching
         */
        @ConfigurationParameter(description = "Whether to lowercase labels before matching", defaultValue="true")
        public boolean lowercase = true;
    }

    /**
     * The mode of matching labels
     */
    public static enum Mode {
        /**
         * Label is exactly equivalent
         */
        strict,
        /**
         * Any words in common
         */
        lenient
    };

    private static class LabelMatchImpl implements BlockingStrategy {

        private final String leftProperty, rightProperty;
        private final Language language;
        private final Mode mode;
        private final boolean lowercase;

        public LabelMatchImpl(String leftProperty, String rightProperty, Language language, Mode mode, boolean lowercase) {
            this.leftProperty = leftProperty;
            this.rightProperty = rightProperty;
            this.language = language;
            this.mode = mode;
            this.lowercase = lowercase;
        }

        @Override
        @SuppressWarnings("Convert2Lambda")
        public Iterable<Pair<Resource, Resource>> block(Dataset _left, Dataset _right, NaiscListener log) {
            final Model left = _left.asModel().getOrExcept(new RuntimeException("Cannot apply method to SPARQL endpoint"));
            final Model right = _right.asModel().getOrExcept(new RuntimeException("Cannot apply method to SPARQL endpoint"));
            Property leftProp = left.createProperty(leftProperty);
            Property rightProp = right.createProperty(rightProperty);

            final Map<String, List<Resource>> leftLabels = new HashMap<>();
            extractLabel(leftLabels, left, leftProp);
            if(leftLabels.isEmpty()) {
                log.message(NaiscListener.Stage.BLOCKING, NaiscListener.Level.CRITICAL, "No URIs in the left dataset have the property " + leftProperty);
            }

            final Map<String, List<Resource>> rightLabels = new HashMap<>();
            extractLabel(rightLabels, right, rightProp);
            if(rightLabels.isEmpty()) {
                log.message(NaiscListener.Stage.BLOCKING, NaiscListener.Level.CRITICAL, "No URIs in the right dataset have the property " + rightProperty);
            }

            return new Iterable<Pair<Resource, Resource>>() {
                @Override
                public Iterator<Pair<Resource, Resource>> iterator() {
                    return leftLabels.entrySet().stream().flatMap(k -> {
                        if (rightLabels.containsKey(k.getKey())) {
                            return k.getValue().stream().flatMap(l
                                    -> rightLabels.get(k.getKey()).stream().map(r
                                            -> new Pair(l, r)));
                        } else {
                            return Collections.EMPTY_LIST.stream();
                        }
                    }).iterator();
                }
            };
        }

        private void extractLabel(Map<String, List<Resource>> map, Model model, Property prop) {
            StmtIterator iter = model.listStatements(null, prop, (RDFNode) null);
            while (iter.hasNext()) {
                Statement stmt = iter.next();
                if (stmt.getObject().isLiteral() && (language == null
                        || language.equals(getLang(stmt.getObject())))) {
                    Resource subj = stmt.getSubject();
                    if (subj.isURIResource()) {
                        String[] indexables = indexables(stmt.getObject().asLiteral().getString(), mode, lowercase);
                        for (String indexable : indexables) {
                            if (!map.containsKey(indexable)) {
                                map.put(indexable, new ArrayList<>());
                            }
                            map.get(indexable).add(subj);
                        }
                    }
                }
            }
        }

    }

    static String[] indexables(String input, Mode mode, boolean lowercase) {
        if (mode == Mode.strict) {
            return new String[]{lowercase ? input.toLowerCase() : input};
        } else {
            return PrettyGoodTokenizer.tokenize(lowercase ? input.toLowerCase() : input);
        }
    }

    private static Language getLang(RDFNode node) {
        Literal lit = node.asLiteral();
        String ls = lit.getLanguage();
        if (ls == null || ls.equals("")) {
            return null;
        } else {
            return Language.get(ls).getLanguageOnly();
        }
    }
}
