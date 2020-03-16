package org.insightcentre.uld.naisc.feature.embeddings;

/**
 * A tokenized sentence.
 * 
 * @author John McCrae
 */
public class Sentence {
    public final String[] tokens;

    public Sentence(String[] tokens) {
        this.tokens = tokens;
    }

    /**
     * The length of this sentence in tokens
     * @return A non-negative number
     */
    public int size() { return tokens.length; }
}
