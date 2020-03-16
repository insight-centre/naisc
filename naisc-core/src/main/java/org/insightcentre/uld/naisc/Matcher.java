package org.insightcentre.uld.naisc;

import org.insightcentre.uld.naisc.main.ExecuteListener;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.NONE;

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
    default AlignmentSet align(AlignmentSet matches) {
        return alignWith(matches, new AlignmentSet(), ExecuteListeners.NONE);
    }
    
    /**
     * Match two schemas
     * @param matches The set of all scored matches
     * @param listener The listener to use
     * @return A subset of matches optimized according to the rules of this matcher
     */
    default AlignmentSet align(AlignmentSet matches, ExecuteListener listener) {
        return alignWith(matches, new AlignmentSet(), listener);
    }
    
    /**
     * Match two schemas with a fixed list of known matches
     * @param matches The set of all scored matches
     * @param partialAlign The partial solution that must appear in the complete solution
     * @param listener The listener to use
     * @return  A subset of matches and partial align optimized according to the rules of this matcher
     */
    AlignmentSet alignWith(AlignmentSet matches, AlignmentSet partialAlign, ExecuteListener listener);
}
