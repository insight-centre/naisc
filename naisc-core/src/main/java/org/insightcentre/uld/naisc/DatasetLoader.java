package org.insightcentre.uld.naisc;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Interface to load a dataset
 * @author John McCrae
 */
public interface DatasetLoader<Combinable extends Dataset> {
    /**
     * Create a dataset from a RDF file
     * @param file The file to load
     * @param name The name of the dataset
     * @return The dataset object
     * @throws IOException if the file cannot be loaded
     */
    Dataset fromFile(File file, String name) throws IOException;
    
    /**
     * Create a dataset from a SPARQL endpoint
     * @param endpoint The endpoint to query
     * @return The dataset object
     */
    Dataset fromEndpoint(URL endpoint);
    
    /**
     * Combine two datasets
     * @param dataset1 The first dataset
     * @param dataset2 The second dataset
     * @param name The name of the combined dataset
     * @return The combination of the two datasets
     */
    Combinable combine(Combinable dataset1, Combinable dataset2, String name);

}
