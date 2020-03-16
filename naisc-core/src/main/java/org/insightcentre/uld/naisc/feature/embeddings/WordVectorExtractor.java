package org.insightcentre.uld.naisc.feature.embeddings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * The extractor for word vectors
 * 
 * @author John McCrae
 */
public class WordVectorExtractor {

    private final Word2Vector w2vec;

    public WordVectorExtractor(String embeddingPath) {
        w2vec = new GloveVectors(embeddingPath);
    }

    public SentenceVectors extractFeatures(String[] sentence) {
        double[][] vectors = new double[sentence.length][];
        for (int i = 0; i < sentence.length; i++) {
            double[] vector = w2vec.getWordVector(sentence[i].trim().toLowerCase());
            vectors[i] = vector;
        }
        return new SentenceVectors(vectors, sentence);
    }

    /**
     * A word 2 vector implementation
     *
     * @author Kartik Asooja
     */
    private interface Word2Vector {

        public double[] getWordVector(String word);

    }

    /**
     * Load glove vectors
     *
     * @author Kartik Asooja
     */
    public class GloveVectors implements Word2Vector {

        public Map<String, double[]> vecs = new HashMap<String, double[]>();
        public String filePath = "src/test/resources/glove.6B.50d.txt";
        public int n = -1;

        public GloveVectors(String filePath) {
            this.filePath = filePath;
            loadEmbeddings(this.filePath);
        }
        
        

        private void loadEmbeddings(String filePath) {
            if (!new java.io.File(filePath).exists()) {
                throw new IllegalArgumentException(filePath + " does not exist");
            }
            BufferedReader br; 
            try {
                br = new BufferedReader(new FileReader(filePath));
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
            String line = null;
            try {
                while ((line = br.readLine()) != null) {
                    String[] split = line.split("\\s+");
                    String word = split[0].trim();
                    int i = 1;
                    double[] vec = new double[split.length - 1];
                    for (i = 1; i < split.length; i++) {
                        vec[i - 1] = Double.parseDouble(split[i]);
                    }
                    if (n >= 0 && n != vec.length) {
                        throw new RuntimeException("Inconsistent vector length");
                    }
                    n = vec.length;
                    vecs.put(word, vec);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public double[] getWordVector(String word) {
            double[] vec = vecs.get(word);
            if (vec == null) {
                vec = new double[n];
            }
            return vec;
        }

    }

}
