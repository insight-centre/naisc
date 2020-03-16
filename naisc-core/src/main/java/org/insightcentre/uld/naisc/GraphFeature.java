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
    default double[] extractFeatures(Resource entity1, Resource entity2) {
        return extractFeatures(entity1, entity2, NaiscListener.DEFAULT);
    }
        
    /**
     * Extract features from a feature extractor
     * @param entity1 The entity from the left dataset
     * @param entity2 The entity from the right dataset
     * @param log The listener
     * @return The set of features
     */
    double[] extractFeatures(Resource entity1, Resource entity2, NaiscListener log);

    /**
     * Get the names of features that are generated by this classifier
     * @return The list of feature names
     */
    String[] getFeatureNames();
}
