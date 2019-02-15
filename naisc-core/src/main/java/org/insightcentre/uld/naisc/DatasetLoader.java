package org.insightcentre.uld.naisc;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Interface to load a dataset
 * @author John McCrae
 */
public interface DatasetLoader {
    /**
     * Create a dataset from a RDF file
     * @param file The file to load
     * @return The dataset object
     * @throws IOException if the file cannot be loaded
     */
    Dataset fromFile(File file) throws IOException;
    
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
     * @return The combination of the two datasets
     */
    Dataset combine(Dataset dataset1, Dataset dataset2);

}
