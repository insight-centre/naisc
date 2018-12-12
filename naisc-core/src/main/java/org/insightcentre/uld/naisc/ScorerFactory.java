package org.insightcentre.uld.naisc;

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
     * @return The similarity classifier
     */
    Scorer makeScorer(Map<String, Object> params);
    
    /**
     * Get the trainer for this scorer.
     * @param params The configuration of this scorer
     * @return A scorer instance or None for untrainable scorers
     */
    Option<ScorerTrainer> makeTrainer(Map<String, Object> params);
}
