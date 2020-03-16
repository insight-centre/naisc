package org.insightcentre.uld.naisc;

import java.util.Map;
import java.util.Set;

/**
 * A factory for creating text feature extractors
 * 
 * @author John McCrae
 */
public interface TextFeatureFactory {
    
    /**
     * Make a feature extractor
     * @param tags The tags of labels to work from (or null for all)
     * @param params The parameters passed to the feature extractor
     * @return The feature extractor
     */
    TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params);
    
}
