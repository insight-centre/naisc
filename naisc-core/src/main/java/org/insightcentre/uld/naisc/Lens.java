package org.insightcentre.uld.naisc;

import org.insightcentre.uld.naisc.util.LangStringPair;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.util.Option;

import java.util.Collection;

/**
 * A Lens extracts a single feature out of an entity that can then be used for 
 * a similarity feature
 * 
 * @author John McCrae
 */
public interface Lens {
    /**
     * Extract from a lens
     * @param entity1 The left entity to extract from
     * @param entity2 The right entity to extract from
     * @return The pair of labels extracted by this lens or None if no label could be extracted
     */
    default Collection<LensResult> extract(URIRes entity1, URIRes entity2) {
        return extract(entity1, entity2, NaiscListener.DEFAULT);
    }
    
    /**
     * Extract from a lens
     * @param entity1 The left entity to extract from
     * @param entity2 The right entity to extract from
     * @param log The listener
     * @return The pair of labels extracted by this lens or None if no label could be extracted
     */
    Collection<LensResult> extract(URIRes entity1, URIRes entity2, NaiscListener log);
}
