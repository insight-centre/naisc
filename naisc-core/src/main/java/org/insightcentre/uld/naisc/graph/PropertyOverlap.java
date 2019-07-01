package org.insightcentre.uld.naisc.graph;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.GraphFeatureFactory;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.util.StringPair;

/**
 * Computes the overlap of the triples for which the two elements share by
 * means of Jaccard and Dice.
 * 
 * @author John McCrae
 */
public class PropertyOverlap implements GraphFeatureFactory {

    @Override
    public GraphFeature makeFeature(Dataset dataset, Map<String, Object> params) {
        dataset.asModel().getOrExcept(new RuntimeException("Cannot apply method to SPARQL endpoint"));
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        return new PropertyOverlapImpl(config.properties);
    }

    /** Configuration for the property overlap feature */
    public static class Configuration {
        /** The set of properties to use for overlap or empty for no properties */
        @ConfigurationParameter(description = "The set of properties to use for overlap or empty for no properties")
        public Set<String> properties;        
    }
    
    private static class PropertyOverlapImpl implements GraphFeature  {
        private final Set<String> properties;

        public PropertyOverlapImpl(Set<String> properties) {
            this.properties = properties;
        }
        

        @Override
        public String id() {
            return "property-overlap";
        }

        @Override
        public double[] extractFeatures(Resource entity1, Resource entity2, NaiscListener log) {
            Set<StringPair> lvals = new HashSet<>();
            StmtIterator iter = entity1.listProperties();
            while(iter.hasNext()) {
                Statement stmt = iter.next();
                if(properties == null || properties.contains(stmt.getPredicate().getURI())) {
                    lvals.add(new StringPair(stmt.getPredicate().getURI(), toN3(stmt.getObject())));
                }
            }
            Set<StringPair> rvals = new HashSet<>();
            iter = entity2.listProperties();
            while(iter.hasNext()) {
                Statement stmt = iter.next();
                if(properties == null || properties.contains(stmt.getPredicate().getURI())) {
                    rvals.add(new StringPair(stmt.getPredicate().getURI(), toN3(stmt.getObject())));
                }
            }
            double A = lvals.size();
            double B = rvals.size();
            lvals.retainAll(rvals);
            double AB = lvals.size();
            
            double dice = 2.0 * AB / (A + B);
            double jaccard = AB / (A + B - AB);
            return new double[]{dice, jaccard};
        }

        @Override
        public String[] getFeatureNames() {
            return new String[] { "property-overlap-jaccard", "property-overlap-dice" };
        }
    }
    
    private static String toN3(RDFNode node) {
        if(node.isURIResource()) {
            return "<" + node.asResource().getURI() + ">";
        } else if (node.isAnon()) {
            return "_:" + node.asResource().getId().toString();
        } else if(node.isLiteral()) {
            Literal l = node.asLiteral();
            String s = "\"" + l.getLexicalForm() + "\"";
            if(l.getLanguage() != null && !"".equals(l.getLanguage())) {
                return s + "@" + l.getLanguage();
            } else if(l.getDatatypeURI() != null && !"".equals(l.getDatatypeURI())) {
                return s + "^^<" + l.getDatatypeURI() + ">";
            } else {
                return s;
            }
        } else {
            throw new IllegalArgumentException("Node must be resource or literal!?");
        }
    }
}
