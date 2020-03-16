package org.insightcentre.uld.naisc;

import java.util.Map;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.util.Lazy;

/**
 * Interface for creating blocking strategies
 * 
 * @author John McCrae
 */
public interface BlockingStrategyFactory {
    /**
     * Create a new instance of a blocking strategy
     * 
     * @param params The configuration of this object
     * @param analysis The analysis of the problem
     * @param listener A listener to record any notes
     * @return A new instance of a blocking strategy
     */
    BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> analysis, NaiscListener listener);
}
