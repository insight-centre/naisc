package org.insightcentre.uld.naisc.lens;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.Labels;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * Extract a single label with a known URL
 *
 * @author John McCrae
 */
public class Label implements LensFactory {

    private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public Lens makeLens(Dataset dataset, Map<String, Object> params) {
        Configuration config = mapper.convertValue(params, Configuration.class);
        return new LabelImpl(config.property, config.rightProperty == null ? config.property : config.rightProperty, config.language, dataset, config.id);
    }

    static class LabelImpl implements Lens {

        private final Property leftProp, rightProp;
        private final Language language;
        private final Dataset model;
        private final String id;

        public LabelImpl(String leftProperty, String rightProperty, String language, Dataset model,
                String id) {
            this.leftProp = model.createProperty(leftProperty);
            this.rightProp = model.createProperty(rightProperty);
            this.language = language == null ? null : Language.get(language);
            this.model = model;
            this.id = id;
        }

        public String id() {
            if (id != null) {
                return id;
            }
            StringBuilder sb = new StringBuilder("label");
            if (language != null) {
                sb.append("-").append(language);
            }
            if (!leftProp.getURI().equals(RDFS_LABEL)) {
                sb.append("-").append(String.format("-%d", leftProp.hashCode()));
            }
            return sb.toString();
        }

        @Override
        public Collection<LensResult> extract(URIRes res1, URIRes res2, NaiscListener log) {
            List<Literal> lit1 = new ArrayList<>();
            Resource entity1 = res1.toJena(model);
            Resource entity2 = res2.toJena(model);

            NodeIterator iter1 = model.listObjectsOfProperty(entity1, leftProp);
            while (iter1.hasNext()) {
                RDFNode node1 = iter1.next();
                if (node1.isLiteral()) {
                    lit1.add(node1.asLiteral());
                }
            }

            List<Literal> lit2 = new ArrayList<>();
            NodeIterator iter2 = model.listObjectsOfProperty(entity2, rightProp);
            while (iter2.hasNext()) {
                RDFNode node2 = iter2.next();
                if (node2.isLiteral()) {
                    lit2.add(node2.asLiteral());
                }

            }
            List<LangStringPair> labels = Labels.closestLabelsByLang(lit1, lit2);

            for (LangStringPair label : labels) {
                if (language == null || label.lang1.equals(language)) {
                    return new Some<>(LensResult.fromLangStringPair(label,id()));
                }
            }
            return new None<>();
        }
    }

    public static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
    public static final String SKOS_PREFLABEL = "http://www.w3.org/2004/02/skos/core#prefLabel";

    /**
     * Configuration of the label lens.
     */
    public static class Configuration {

        /**
         * The property to extract. Default is rdfs:label
         */
        @ConfigurationParameter(description = "The property to extract", defaultValue = "[\"http://www.w3.org/2000/01/rdf-schema#label\"]")
        public String property = RDFS_LABEL;
        /**
         * The property on the right to extract, empty string will use the same
         * property on both models
         */
        @ConfigurationParameter(description = "The property to extract", defaultValue = "[\"http://www.w3.org/2000/01/rdf-schema#label\"]")
        public String rightProperty = null;
        /**
         * The language to extract. Default is <code>null</code> for all
         * languages.
         */
        @ConfigurationParameter(description = "The language to extract", defaultValue = "null")
        public String language = null;

        /**
         * The name for this lens, (no two lenses may have the same name). This
         * is consumed by the text features in order to distinguish features
         * coming from different sources
         */
        @ConfigurationParameter(description = "The unique identifier of this lens")
        public String id = null;
    }
}
