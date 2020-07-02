package org.insightcentre.uld.naisc;

import org.insightcentre.uld.naisc.scorer.ModelNotTrainedException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Predict if two entities are similar
 * @author John McCrae
 */
public interface Scorer extends Closeable {
    /**
     * Predict the similarity of a pair of entities from a set of features
     * @param features The features
     * @return The similarity probability between 0.0 (not at all similar) and 1.0 (exactly the same)
     */
    default List<ScoreResult> similarity(FeatureSet features) throws ModelNotTrainedException {
        return similarity(features, NaiscListener.DEFAULT);
    }
      /**
     * Predict the similarity of a pair of entities from a set of features
     * @param features The features
     * @param log The listener
     * @return The similarity probability between 0.0 (not at all similar) and 1.0 (exactly the same)
     */
    List<ScoreResult> similarity(FeatureSet features, NaiscListener log) throws ModelNotTrainedException;
}
