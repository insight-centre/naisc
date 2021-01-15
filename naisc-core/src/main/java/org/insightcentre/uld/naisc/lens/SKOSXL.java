package org.insightcentre.uld.naisc.lens;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.Labels;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Some;

import java.util.*;

/**
 * Implements a lens that extracts data from SKOS-XL models
 *
 * @author jmccrae
 */
public class SKOSXL implements LensFactory {
    @Override
    public Lens makeLens(Dataset dataset, Map<String, Object> params) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if(config.leftLabel != null && config.rightLabel != null) {
            System.err.println("SKOS-XL lens in use, but setting both label properties ignores SKOS-XL!");
        }
        return new SKOSXLImpl(config.leftLabel, config.rightLabel, dataset, config.language == null ? null : Language.get(config.language));
    }

    /**
     * Configuration class
     */
    @ConfigurationClass("Extract text from SKOS-XL models. Assumes both datasets use SKOS-XL, to link between a SKOS-XL and a non-SKOS-XL dataset, set one of the label properties")
    public static class Configuration {
        /**
         * If not null, use this property as a normal label for the left dataset
         */
         @ConfigurationParameter(description = "If not null, use this property as a normal label for the left dataset instead of SKOS-XL", defaultValue = "null")
        public String leftLabel = null;

        /**
         * If not null, use this property as a normal label for the right dataset
         */
        @ConfigurationParameter(description = "If not null, use this property as a normal label for the right dataset instead of SKOS-XL", defaultValue = "null")
        public String rightLabel = null;
        /**
         * The language to extract. Default is <code>null</code> for all
         * languages.
         */
        @ConfigurationParameter(description = "The language to extract", defaultValue = "null")
        public String language = null;
    }

    private static List<String> SKOSXL_LABELS = Arrays.asList(
            "http://www.w3.org/2008/05/skos-xl#prefLabel",
            "http://www.w3.org/2008/05/skos-xl#altLabel");


    private static class SKOSXLImpl implements Lens {
        private final String leftLabel, rightLabel;
        private final Dataset dataset;
        private final Language language;

        public SKOSXLImpl(String leftLabel, String rightLabel, Dataset dataset, Language language) {
            this.leftLabel = leftLabel;
            this.rightLabel = rightLabel;
            this.dataset = dataset;
            this.language = language;
        }

        @Override
        public Collection<LensResult> extract(URIRes res1, URIRes res2, NaiscListener log) {
            Resource entity1 = res1.toJena(dataset);
            Resource entity2 = res2.toJena(dataset);

            List<Literal> lit1 = new ArrayList<>();
            if(leftLabel == null) {
                for(String skosxlLabel : SKOSXL_LABELS) {
                    NodeIterator nodeIterator = dataset.listObjectsOfProperty(entity1, dataset.createProperty(skosxlLabel));
                    while(nodeIterator.hasNext()) {
                        RDFNode n = nodeIterator.next();
                        if(n.isResource()) {
                            NodeIterator nodeIterator1 = dataset.listObjectsOfProperty(n.asResource(), dataset.createProperty("http://www.w3.org/2008/05/skos-xl#literalForm"));
                            while(nodeIterator1.hasNext()) {
                                RDFNode n2 = nodeIterator1.next();
                                if(n2.isLiteral()) {
                                    lit1.add(n2.asLiteral());
                                }
                            }
                        }
                    }
                }
            } else {
                NodeIterator iter1 = dataset.listObjectsOfProperty(entity1, dataset.createProperty(leftLabel));
                while (iter1.hasNext()) {
                    RDFNode node1 = iter1.next();
                    if (node1.isLiteral()) {
                        lit1.add(node1.asLiteral());
                    }
                }

            }
            List<Literal> lit2 = new ArrayList<>();
            if(rightLabel == null) {
                for(String skosxlLabel : SKOSXL_LABELS) {
                    NodeIterator nodeIterator = dataset.listObjectsOfProperty(entity2, dataset.createProperty(skosxlLabel));
                    while(nodeIterator.hasNext()) {
                        RDFNode n = nodeIterator.next();
                        if(n.isResource()) {
                            NodeIterator nodeIterator1 = dataset.listObjectsOfProperty(n.asResource(), dataset.createProperty("http://www.w3.org/2008/05/skos-xl#literalForm"));
                            while(nodeIterator1.hasNext()) {
                                RDFNode n2 = nodeIterator1.next();
                                if(n2.isLiteral()) {
                                    lit2.add(n2.asLiteral());
                                }
                            }
                        }
                    }
                }
            } else {
                NodeIterator iter2 = dataset.listObjectsOfProperty(entity2, dataset.createProperty(rightLabel));
                while (iter2.hasNext()) {
                    RDFNode node2 = iter2.next();
                    if (node2.isLiteral()) {
                        lit2.add(node2.asLiteral());
                    }

                }
            }

            if(lit1.isEmpty() || lit2.isEmpty()) {
                log.message(NaiscListener.Stage.MATCHING, NaiscListener.Level.WARNING, String.format("No SKOS-XL labels for pair %s <-> %s", entity1.getURI(), entity2.getURI()));
            }
            List<LangStringPair> labels = Labels.closestLabelsByLang(lit1, lit2);

            for (LangStringPair label : labels) {
                if (language == null || label.lang1.equals(language)) {
                    return new Some<>(LensResult.fromLangStringPair(label,"skos-xl"));
                }
            }
            return new None<>();
        }
    }
}
