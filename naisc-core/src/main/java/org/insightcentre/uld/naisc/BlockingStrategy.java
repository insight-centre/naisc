package org.insightcentre.uld.naisc;

import java.util.Collection;
import java.util.Iterator;
import org.insightcentre.uld.naisc.util.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * A strategy for selecting the set of elements to be
 *
 * @author John McCrae
 */
public interface BlockingStrategy {

    /**
     * Extract the set of elements that can be matched in a particular pair of
     * RDF documents.
     *
     * @param left The left RDF document to match
     * @param right The right RDF document to match
     * @return A list of all of the pairs where the first element is in left and
     * the right element is in right
     */
    default Collection<Blocking> block(Dataset left, Dataset right) {
        return block(left, right, NaiscListener.DEFAULT);
    }
    
     /**
     * Extract the set of elements that can be matched in a particular pair of
     * RDF documents.
     *
     * @param left The left RDF document to match
     * @param right The right RDF document to match
     * @param log The listener
     * @return A list of all of the pairs where the first element is in left and
     * the right element is in right
     */
    Collection<Blocking> block(Dataset left, Dataset right, NaiscListener log);

    /**
     * Estimate the number of results that blocking will return. Default implementation
     * simply counts the result of block
     *
     * @param left The left RDF document to match
     * @param right The right RDF document to match
     * @return The estimated size
     */
    default int estimateSize(Dataset left, Dataset right) {
        Iterator<Blocking> i = block(left, right).iterator();
        int n = 0;
        while (i.hasNext()) {
            n++;
            i.next();
        }
        return n;
    }
}
