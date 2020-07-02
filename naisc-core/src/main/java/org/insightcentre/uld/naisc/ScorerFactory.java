package org.insightcentre.uld.naisc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.insightcentre.uld.naisc.util.Option;

/**
 * Create a scorer of similarity
 * @author John McCrae
 */
public interface ScorerFactory {
    /**
     * An identifier for this trainer
     * @return The identifier
     */
    String id();

    /**
     * Train the classifier
     * @param params Any extra parameters
     * @param modelPath The path to the model
     * @return The similarity classifiers (by property)
     * @throws IOException If the model could not be read
     */
    Scorer makeScorer(Map<String, Object> params, File modelPath) throws IOException;
    
    /**
     * Get the trainer for this scorer.
     * @param params The configuration of this scorer
     * @param property The property being predicted
     * @param modelPath The path to the model
     * @return A scorer instance or None for untrainable scorers
     */
    Option<ScorerTrainer> makeTrainer(Map<String, Object> params, String property, File modelPath);
}
