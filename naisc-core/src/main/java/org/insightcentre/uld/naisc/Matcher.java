package org.insightcentre.uld.naisc;

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
}
