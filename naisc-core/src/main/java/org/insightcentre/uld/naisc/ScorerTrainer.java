package org.insightcentre.uld.naisc;

import java.io.Closeable;
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
     * @return A trained instance of the scorer
     */
    Scorer train(List<FeatureSetWithScore> dataset);
    
    /**
     * Get the property that this scorer is training on.
     * @return The URI of the property
     */
    String property();
}
