package org.insightcentre.uld.naisc.feature.embeddings;

import java.util.Arrays;

/**
 * An alignment between two phrases on the word level
 * 
 * @author John McCrae
 */
public class WordAlignment {
    private final Sentence sourceSentence, targetSentence;
    private final double[][] alignment;

    /**
     * Create a word alignment
     * @param sourceSentence The left phrase
     * @param targetSentence The right phrase
     * @param alignment The weights of alignment between the words
     */
    public WordAlignment(Sentence sourceSentence, Sentence targetSentence, 
        double[][] alignment) {
        this.sourceSentence = sourceSentence;
        this.targetSentence = targetSentence;
        this.alignment = alignment;
        if(alignment.length != sourceSentence.size()) {
            throw new IllegalArgumentException("source not size of matrix");
        }
        for (double[] alignment1 : alignment) {
            if (alignment1.length != targetSentence.size()) {
                throw new IllegalArgumentException("target not size of matrix");
            }
        }
    }

    public WordAlignment(WordAlignment alignment) {
        this.sourceSentence = alignment.sourceSentence;
        this.targetSentence = alignment.targetSentence;
        this.alignment = alignment.alignment;
    }
    
    public Sentence getSourceSentence() {
        return sourceSentence;
    }
    
    public Sentence getTargetSentence() {
        return targetSentence;
    }

    public int getSourceSize() {
        return sourceSentence.size();
    }

    public int getTargetSize() {
        return targetSentence.size();
    }
    
    /**
     * Get the alignment between the ith word of the source sentence and the jth
     * word of the target sentence
     * @param i The source sentence word index
     * @param j The target sentence word index
     * @return The alignment score
     */
    public double alignment(int i, int j) {
        return alignment[i][j];
    }

    @Override
    public int hashCode() {
        int hash = 7;
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
        final WordAlignment other = (WordAlignment) obj;
        return true;
    }

    @Override
    public String toString() {
        return "Alignment{\n" + "sourceSentence=" + sourceSentence + "\n targetSentence=" + targetSentence + "\n alignment=" + Arrays.deepToString(alignment) + "\n}";
    }

    public boolean isEmpty() {
        return sourceSentence.size() == 0 || targetSentence.size() == 0;
    }
    
}
