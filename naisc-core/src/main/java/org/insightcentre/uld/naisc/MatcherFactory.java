package org.insightcentre.uld.naisc;

import java.util.Map;

/**
 * A training method for a schema aligner
 * 
 * @author John McCrae
 */
public interface MatcherFactory {
    /**
     * An identifier for this trainer
     * @return The identifier
     */
    String id();


    /**
     * Create a matcher
     * @param params The configuration of the matcher
     * @return A matcher instance
     */
    Matcher makeMatcher(Map<String, Object> params);
}
