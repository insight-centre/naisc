package org.insightcentre.uld.naisc.feature.embeddings;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * An aligner between two sets of words
 * @author John McCrae
 */
public interface WordAligner {
    /**
     * Align two sentences
     * @param x The source sentence to align
     * @param y The target sentence to align
     * @return A matrix giving the alignment
     */
    public WordAlignment align(String x, String y);
    
     /**
     * Align two sentences, ignoring stopwords
     * @param x The source sentence to align
     * @param y The target sentence to align
     * @param stopwords The list of stopwords
     * @return A matrix giving the alignment
     */
    public WordAlignment align(String x, String y, Set<String> stopwords);
    
    /**
     * Save the model to a file
     * @param file The file to save the model to
     * @throws java.io.IOException The aligner cannot be saved
     */
    public void save(File file) throws IOException;

    /**
     * A string that identifies this aligner
     * @return The id
     */
    public String id();
}
