package org.insightcentre.uld.naisc;

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
     * @param left The left RDF document to match
     * @param right The right RDF document to match
     * @return A list of all of the pairs where the first element is in left and 
     * the right element is in right
     */
    Iterable<Pair<Resource, Resource>> block(Model left, Model right);
    
}
