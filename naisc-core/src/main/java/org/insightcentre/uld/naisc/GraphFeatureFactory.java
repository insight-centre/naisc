package org.insightcentre.uld.naisc;

import java.util.Map;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.util.Lazy;

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
     * @param analysis The analysis of the dataset
     * @param prelinking The prelinking
     * @param listener A listener for events
     * @return The graph feature
     */
    GraphFeature makeFeature(Dataset sparqlData, Map<String, Object> params,
            Lazy<Analysis> analysis, AlignmentSet prelinking,
            NaiscListener listener);
}
