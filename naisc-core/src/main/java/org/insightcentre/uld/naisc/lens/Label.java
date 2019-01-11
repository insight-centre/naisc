package org.insightcentre.uld.naisc.lens;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.LensFactory;
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

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Lens makeLens(String tag, Model sparqlData, Map<String, Object> params) {
        Configuration config = mapper.convertValue(params, Configuration.class);
        return new LabelImpl(config.property, config.language, tag, sparqlData);
    }

    private static class LabelImpl implements Lens {

        private final List<Property> properties;
        private final Language language;
        private final String tag;
        private final Model model;

        public LabelImpl(List<String> property, String language, String tag, Model model) {
            this.properties = property.stream().map(p -> model.createProperty(p)).collect(Collectors.toList());
            this.language = language == null ? null : Language.get(language);
            this.tag = tag;
            this.model = model;
        }

        @Override
        public String id() {
            StringBuilder sb = new StringBuilder("label");
            if (language != null) {
                sb.append("-").append(language);
            }
            Property property = properties.get(0);
            if (!property.getURI().equals(RDFS_LABEL)) {
                sb.append("-").append(String.format("-%d", property.hashCode()));
            }
            return sb.toString();
        }

        @Override
        public Option<LangStringPair> extract(Resource entity1, Resource entity2) {
            //List<LangStringPair> result = new ArrayList<>();
            for (Property lproperty : properties) {
                NodeIterator iter1 = model.listObjectsOfProperty(entity1, lproperty);
                while (iter1.hasNext()) {
                    RDFNode node1 = iter1.next();
                    Language l1 = getLang(node1);
                    for(Property rproperty : properties) {
                    if (node1.isLiteral() && (language == null || (l1 != null && language.getLanguageOnly().equals(l1.getLanguageOnly())))) {
                        NodeIterator iter2 = model.listObjectsOfProperty(entity2, rproperty);
                        while (iter2.hasNext()) {
                            RDFNode node2 = iter2.next();
                            Language l2 = getLang(node2);
                            if (node2.isLiteral() && (language == null || (l2 != null && language.getLanguageOnly().equals(l2.getLanguageOnly())))) {
                                return new Some<>(new LangStringPair(l1, l2, node1.asLiteral().getString(), node2.asLiteral().getString()));
                            }
                        }
                    }
                    }

                }
            }
            return new None<>();
        }

        @Override
        public String tag() {
            return tag;
        }

    }

    private static Language getLang(RDFNode node) {
        Literal lit = node.asLiteral();
        String ls = lit.getLanguage();
        if (ls == null || ls.equals("")) {
            return null;
        } else {
            return Language.get(ls);
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
        public List<String> property = Collections.singletonList(RDFS_LABEL);
        /**
         * The language to extract. Default is <code>null</code> for all
         * languages.
         */
        @ConfigurationParameter(description = "The language to extract", defaultValue = "null")
        public String language = null;
    }
}
