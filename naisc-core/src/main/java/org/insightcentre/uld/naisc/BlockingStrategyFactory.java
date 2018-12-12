package org.insightcentre.uld.naisc;

import java.util.Map;

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
     * @return A new instance of a blocking strategy
     */
    BlockingStrategy makeBlockingStrategy(Map<String, Object> params);
}
