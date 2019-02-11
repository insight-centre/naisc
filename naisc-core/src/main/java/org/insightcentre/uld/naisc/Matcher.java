package org.insightcentre.uld.naisc;

import org.insightcentre.uld.naisc.main.ExecuteListener;

/**
 * An aligner between two schemas
 * @author John McCrae
 */
public interface Matcher {
    /**
     * Match two schemas
     * @param matches The set of all scored matches
     * @return A subset of matches optimized according to the rules of this matcher
     */
    AlignmentSet align(AlignmentSet matches);
    
    /**
     * Match two schemas
     * @param matches The set of all scored matches
     * @param listener The listener to use
     * @return A subset of matches optimized according to the rules of this matcher
     */
    default AlignmentSet align(AlignmentSet matches, ExecuteListener listener) {
        return align(matches);
    }
}
