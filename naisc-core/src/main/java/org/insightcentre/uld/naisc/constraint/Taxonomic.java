package org.insightcentre.uld.naisc.constraint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.vocabulary.SKOS;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.ConfigurationClass;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.URIRes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements the taxonomic constraint over SKOS-XL models. That is that no node may be linked by multiple types of links,
 * that exact links are bijective, and broder/narrower links are injective/surjective
 *
 * @author John P. McCrae
 */
public class Taxonomic implements ConstraintFactory {
    @Override
    public Constraint make(Map<String, Object> params) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        return new TaxonomicImpl(0.0,  config.exactMatch, config.broadMatch, config.narrowMatch, config.relatedMatch,
            new HashMap<>(), new HashMap<>());
    }

    /**
     * The taxonomic linking configuration
     */
     @ConfigurationClass("The taxonomic linking constraint for exact/broader/narrower/related matching of datasets")
    public static class Configuration {
        /**
         * The property for exact matching
         */
        @ConfigurationParameter(description = "The property for exact matching", defaultValue = "http://www.w3.org/2004/02/skos/core#exactMatch")
        public String exactMatch = SKOS.exactMatch.getURI();
        /**
         * The property for broader matching
         */
        @ConfigurationParameter(description = "The property for exact matching", defaultValue = "http://www.w3.org/2004/02/skos/core#broadMatch")
        public String broadMatch = SKOS.broadMatch.getURI();
        /**
         * The property for narrower matching
         */
        @ConfigurationParameter(description = "The property for exact matching", defaultValue = "http://www.w3.org/2004/02/skos/core#narrowMatch")
        public String narrowMatch = SKOS.narrowMatch.getURI();
        /**
         * The property for related matching
         */
        @ConfigurationParameter(description = "The property for exact matching", defaultValue = "http://www.w3.org/2004/02/skos/core#relatedMatch")
        public String relatedMatch = SKOS.relatedMatch.getURI();
    }

    private static class TaxonomicImpl extends Constraint {
        private final String exactMatch, broadMatch, narrowMatch, relatedMatch;
        final Map<URIRes, List<Alignment>> byLeft;
        final Map<URIRes, List<Alignment>> byRight;

        public TaxonomicImpl(double score, String exactMatch, String broadMatch, String narrowMatch, String relatedMatch, Map<URIRes, List<Alignment>> byLeft, Map<URIRes, List<Alignment>> byRight) {
            super(score);
            this.exactMatch = exactMatch;
            this.broadMatch = broadMatch;
            this.narrowMatch = narrowMatch;
            this.relatedMatch = relatedMatch;
            this.byLeft = byLeft;
            this.byRight = byRight;
        }

        @Override
        public List<Alignment> alignments() {
            return byLeft.values().stream().flatMap((x) -> x.stream()).collect(Collectors.toList());
        }

        @Override
        public boolean canAdd(Alignment alignment) {
            if(alignment.property.equals(exactMatch)) {
                return !byLeft.containsKey(alignment.entity1) && !byRight.containsKey(alignment.entity2);
            } else if(alignment.property.equals(broadMatch)) {
                return !byLeft.containsKey(alignment.entity1) && (!byRight.containsKey(alignment.entity2) ||
                    byRight.get(alignment.entity2).stream().allMatch(p -> p.property.equals(broadMatch)));
            } else if(alignment.property.equals(narrowMatch)) {
                return !byRight.containsKey(alignment.entity2) && (!byLeft.containsKey(alignment.entity1) ||
                    byLeft.get(alignment.entity1).stream().allMatch(p -> p.property.equals(narrowMatch)));
            } else if(alignment.property.equals(relatedMatch)) {
                return (!byRight.containsKey(alignment.entity2) ||
                        byRight.get(alignment.entity2).stream().allMatch(p -> p.property.equals(relatedMatch))) &&
                        (!byLeft.containsKey(alignment.entity1) ||
                                byLeft.get(alignment.entity1).stream().allMatch(p -> p.property.equals(relatedMatch)));
            } else {
                throw new IllegalArgumentException(String.format("Attempt to add alignment of type %s not supported by taxonomic linking", alignment.property));
            }
        }

        @Override
        public void add(Alignment alignment) {
            score += delta(alignment);
            if(!byLeft.containsKey(alignment.entity1)) {
                byLeft.put(alignment.entity1, new ArrayList<>());
            }
            byLeft.get(alignment.entity1).add(alignment);

            if(!byRight.containsKey(alignment.entity2)) {
                byRight.put(alignment.entity2, new ArrayList<>());
            }
            byRight.get(alignment.entity2).add(alignment);
        }

        @Override
        public Constraint copy() {
            Map<URIRes, List<Alignment>> newByLeft = new HashMap<>(byLeft);
            Map<URIRes, List<Alignment>> newByRight = new HashMap<>(byRight);
            return new TaxonomicImpl(score, exactMatch, broadMatch, narrowMatch, relatedMatch, byLeft, byRight);
        }
    }
}
