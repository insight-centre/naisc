package org.insightcentre.uld.naisc;

import java.net.URL;
import org.apache.jena.rdf.model.Model;
import org.insightcentre.uld.naisc.util.Option;

/**
 * Get a dataset either as a model or if available as a SPARQL endpoint URL
 * 
 * @author John McCrae
 */
public interface Dataset {
    /**
     * Return this dataset as a Jena model
     * @return The Jena Model (if available)
     */
    Option<Model> asModel();
    /**
     * Return this dataset as a SPARQL endpoint
     * @return The endpoint URL (if available)
     */
    Option<URL> asEndpoint();

}
