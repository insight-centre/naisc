package org.insightcentre.uld.naisc;

import java.util.Map;
import org.apache.jena.rdf.model.Model;

/**
 * The factory for creating a graph feature extractor
 * 
 * @author John McCrae
 */
public interface GraphFeatureFactory {
    /**
     * Make a data feature
     * @param sparqlData The data containing the two elements
     * @param params Configuration parameters for this lens
     * @return The graph feature
     */
    GraphFeature makeFeature(Model sparqlData, Map<String, Object> params);
}
