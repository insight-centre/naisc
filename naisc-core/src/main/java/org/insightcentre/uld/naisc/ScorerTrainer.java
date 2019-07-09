package org.insightcentre.uld.naisc;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A scorer that can be trained
 * 
 * @author John McCrae
 */
public interface ScorerTrainer extends Closeable {
    /**
     * Score a trainer. This method should also save the scorer to disk in the
     * manner described in its configuration
     * @param dataset The dataset to be trained on
     * @param log The logger for training messages
     * @return A trained instance of the scorer
     */
    Scorer train(List<FeatureSetWithScore> dataset, NaiscListener log);
    
    /**
     * Get the property that this scorer is training on.
     * @return The URI of the property
     */
    String property();
        
    /**
     * Save the scorer to a path (used after training)
     * @param scorer The scorer to save
     * @throws IOException If the scorer could not be saved
     * @throws IllegalArgumentException If the scorer was not created by this trainer
     */
    void save(Scorer scorer) throws IOException;
}
