package org.insightcentre.uld.naisc;

import org.apache.jena.rdf.model.Resource;

/**
 * Extracts features from two entities. This is a generalization that does 
 * not depend on lenses
 * 
 * @author John McCrae
 */
public interface GraphFeature {
    
    /**
     * Get an ID for the feature extractor
     * @return The ID
     */
    String id();
    
    /**
     * Extract features from a feature extractor
     * @param entity1 The entity from the left dataset
     * @param entity2 The entity from the right dataset
     * @return The set of features
     */
    default Feature[] extractFeatures(URIRes entity1, URIRes entity2) {
        return extractFeatures(entity1, entity2, NaiscListener.DEFAULT);
    }
        
    /**
     * Extract features from a feature extractor
     * @param entity1 The entity from the left dataset
     * @param entity2 The entity from the right dataset
     * @param log The listener
     * @return The set of features
     */
    Feature[] extractFeatures(URIRes entity1, URIRes entity2, NaiscListener log);
}
