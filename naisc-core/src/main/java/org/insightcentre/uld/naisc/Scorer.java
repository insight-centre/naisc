package org.insightcentre.uld.naisc;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Predict if two entities are similar
 * @author John McCrae
 */
public interface Scorer extends Closeable {
    /**
     * Predict the similarity of a pair of entities from a set of features
     * @param features The features
     * @return The similarity score between 0.0 (not at all similar) and 1.0 (exactly the same)
     */
    default ScoreResult similarity(FeatureSet features) {
        return similarity(features, NaiscListener.DEFAULT);
    }
      /**
     * Predict the similarity of a pair of entities from a set of features
     * @param features The features
     * @param log The listener
     * @return The similarity score between 0.0 (not at all similar) and 1.0 (exactly the same)
     */
    ScoreResult similarity(FeatureSet features, NaiscListener log);
      
    /**
     * Get the relation that is predicted by this scorer
     * @return The URI of the relation to be predicted
     */
    String relation();

}
