package org.insightcentre.uld.naisc;

import org.insightcentre.uld.naisc.util.LangStringPair;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.util.Option;

/**
 * A Lens extracts a single feature out of an entity that can then be used for 
 * a similarity feature
 * 
 * @author John McCrae
 */
public interface Lens {
    /**
     * Get an ID for this lens
     * @return An identifier for this lens
     */
    String id();
    /**
     * Extract from a lens
     * @param entity1 The left entity to extract from
     * @param entity2 The right entity to extract from
     * @return The pair of labels extracted by this lens or None if no label could be extracted
     */
    Option<LangStringPair> extract(Resource entity1, Resource entity2);
   /**
    * Get a tag associated with this lens, this is used to select the features that are 
    * generated based on this lens
    * @return The tag
    */
    String tag();
}
