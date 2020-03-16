package org.insightcentre.uld.naisc.feature.embeddings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import org.insightcentre.uld.naisc.util.PrettyGoodTokenizer;
import org.insightcentre.uld.naisc.util.Vectors;

/**
 * Produce a word-alignment based on cosine similarity
 * @author John McCrae
 */
public class CosineSimAligner implements WordAligner {
    private final WordVectorExtractor wve;


    public CosineSimAligner(WordVectorExtractor wve) {
        this.wve = wve;
    }
    
    
    @Override
    public WordAlignment align(String x, String y) {
        String[] s1 = PrettyGoodTokenizer.tokenize(x);
        String[] s2 = PrettyGoodTokenizer.tokenize(y);

        SentenceVectors sv1 = wve.extractFeatures(s1);
        SentenceVectors sv2 = wve.extractFeatures(s2);

        return doAlign(sv1, sv2);
    }

    @Override
    public WordAlignment align(String x, String y, Set<String> stopwords) {
        ArrayList<String> s1 = new ArrayList<>(Arrays.asList(PrettyGoodTokenizer.tokenize(x)));
        ArrayList<String> s2 = new ArrayList<>(Arrays.asList(PrettyGoodTokenizer.tokenize(y)));

        removeStopWords(s1, stopwords);
        removeStopWords(s2, stopwords);

        SentenceVectors sv1 = wve.extractFeatures(s1.toArray(new String[s1.size()]));
        SentenceVectors sv2 = wve.extractFeatures(s2.toArray(new String[s2.size()]));

        return doAlign(sv1, sv2);
    }


    protected WordAlignment doAlign(SentenceVectors x, SentenceVectors y) {
        double[][] aligns = new double[x.size()][y.size()];
        for(int i = 0; i < x.size(); i++) {
            for(int j = 0; j < y.size(); j++) {
                aligns[i][j] = Vectors.cosine(x.vector(i), y.vector(j));
            }
        }
        return new WordAlignment(x, y, aligns);
    }

    @Override
    public void save(File file) throws IOException {
    }

    @Override
    public String id() {
        return "cosine";
    }
    
    private void removeStopWords(ArrayList<String> s1, Set<String> stopwords) {
        Iterator<String> i = s1.iterator();
        while(i.hasNext()) 
            if(stopwords.contains(i.next().toLowerCase()))
                i.remove();
    }
}
