package org.insightcentre.uld.naisc.feature.embeddings;

import org.insightcentre.uld.naisc.util.Vector;
import java.util.Arrays;

/**
 * A sentence with a vector representation (word embedding) for each word
 * @author John McCrae
 */
public class SentenceVectors extends Sentence {
    private final Vector[] vectors;

    private static Vector[] makeDenseVectors(double[][] vec) {
        Vector[] v = new Vector[vec.length];
        for(int i = 0; i < vec.length; i++) {
            v[i] = new DenseVector(vec[i]);
        }
        return v;
    }
    
    public SentenceVectors(double[][] vectors, String[] words) {
        this(makeDenseVectors(vectors), words);
    }
    
    public SentenceVectors(Vector[] vectors, String[] words) {
        super(words);
        this.vectors = vectors;
     //   this.original = original;
        for(int i = 1; i < vectors.length; i++) {
            if(vectors[i].size() != vectors[0].size()) {
                throw new IllegalArgumentException("Vectors are not all the same length");
            }
        }
        if(words.length != vectors.length) {
            throw new IllegalArgumentException("Vectors and words do not have same alignment");
        }
    }
    
    public final String word(int i) {
        return tokens[i];
    }

    public final Vector vector(int i) {
        return vectors[i];
    }
    
    public final String[] words() {
        return tokens;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Arrays.deepHashCode(this.vectors);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SentenceVectors other = (SentenceVectors) obj;
        if (!Arrays.deepEquals(this.vectors, other.vectors)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        for(int i = 0; i < size(); i++) {
            if(i > 0)
                sb.append(" ");
            sb.append(tokens[i]);
            sb.append(" [");
            for(int j = 0; j < Math.min(vectors[i].size(), 6); j++) {
                sb.append(String.format("%.3f", vectors[i].getDouble(j)));
                sb.append(" ");
            }
            if(vectors[i].size() > 6) 
                sb.append("...");
            sb.append("]");
        }
        return sb.toString();
    }
    
}
