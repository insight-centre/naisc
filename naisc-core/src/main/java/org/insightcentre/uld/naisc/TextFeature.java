package org.insightcentre.uld.naisc;

import org.insightcentre.uld.naisc.util.LangStringPair;
import java.io.Closeable;
import java.util.Set;

/**
 * Extracts features from two entity's text
 * @author John McCrae
 */
public interface TextFeature extends Closeable {

    /**
     * Get an ID for the feature extractor
     * @return The ID
     */
    String id();
    
    /**
     * Extract features from a feature extractor
     * @param facet The facet to extract features for
     * @return The set of features
     */
    default Feature[] extractFeatures(LensResult facet) {
        return extractFeatures(facet, NaiscListener.DEFAULT);
    }
    
    /**
     * Extract features from a feature extractor
     * @param facet The facet to extract features for
     * @param log The listener
     * @return The set of features
     */
    Feature[] extractFeatures(LensResult facet, NaiscListener log);

    /**
     * Get the tags that this feature extractor accepts or null for all
     * @return The set of features accepted or null for all
     */
    Set<String> tags();
}
