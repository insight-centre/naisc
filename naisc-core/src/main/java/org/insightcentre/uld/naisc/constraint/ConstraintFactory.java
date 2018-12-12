package org.insightcentre.uld.naisc.constraint;

import java.util.Map;

/**
 * An interface for creating constraints
 * 
 * @author John McCrae
 */
public interface ConstraintFactory {
   
    /**
     * Create an empty (or minimal valid) constraint
     * @param params The configuration
     * @return A new empty topological score
     */
    Constraint make(Map<String, Object> params);
}
