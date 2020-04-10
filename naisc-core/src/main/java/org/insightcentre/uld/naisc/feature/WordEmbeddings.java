package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.feature.embeddings.CosineSimAligner;
import org.insightcentre.uld.naisc.feature.embeddings.SaliencyFeatures;
import org.insightcentre.uld.naisc.feature.embeddings.StandardWordAlignmentFeatureExtractor;
import org.insightcentre.uld.naisc.feature.embeddings.WordAligner;
import org.insightcentre.uld.naisc.feature.embeddings.WordVectorExtractor;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.LangStringPair;

/**
 * Word embedding based features. The idea of this feature extractor is that 
 * it first constructs a similarity image. This images is constructed by computing 
 * the cosine of each word embedding vector to create a matrix whose size 
 * corresponds to the length of each string.
 * 
 * Then a series of features are computed from this image. These features are 
 * invariant of the size of the image. They are as follows
 * 
 * <ul>
 * <li>fp: Forward Proportion, the number of words in the left sentence who 
 * have a single value consuming more than 50% of the probability</li>
 * <li>bp: Backward Proportion, as forward but over the right sentence</li>
 * <li>ham: Harmonic Alignment Mean</li>
 * <li>max: Average max of alignment probability to left sentence</li>
 * <li>max2: Average square of max of alignment probability to right sentence</li>
 * <li>max.5: Average square root of max of alignment probability to right sentence</li>
 * <li>max.1: Average tenth root of max of alignment probability to right sentence</li>
 * <li>collp2: Column mean squared</li>
 * <li>collp10: Columns mean to the tenth power</li>
 * <li>Hg: Gaussian Entropy Diversity</li>
 * <li>salmax: Column saliency maximum</li>
 * <li>salmaxi: Row saliency maximum</li>
 * </ul>
 *
 * @author John McCrae
 */
public class WordEmbeddings implements TextFeatureFactory {

    public static final String[] DEFAULT_FEATURES = new String[]{"fp", "bp", "ham", "max", "max2", "max.5",
        "max.1", "collp2", "collp10", "Hg"};//, "salmax", "salmaxi"};

    /**
     * Configuration for word embeddings.
     */
    public static class Configuration {

        /**
         * The path to the embeddings file.
         */
        @ConfigurationParameter(description = "The path to the embeddings file")
        public String embeddingPath;
        /**
         * The features to use. Values include "fp", "bp", "ham", "max", "max2", "max.5", "max.1", "collp2", "collp10", "Hg"
         */
        @ConfigurationParameter(description = "The features to use; values include \"fp\", \"bp\", \"ham\", \"max\", \"max2\", \"max.5\",\n" +
"        \"max.1\", \"collp2\", \"collp10\", \"Hg\"")
        public List<String> features = Arrays.asList(DEFAULT_FEATURES);
        /**
         * The path to the saliency values.
         */
        @ConfigurationParameter(description = "The path to the saliency values")
        public String saliencyFile;
        /**
         * The stopwords file (if used).
         */
        @ConfigurationParameter(description = "The stopwords file (if used)")
        public String stopwords;
    }

    @Override
    public TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if (config.embeddingPath == null || !new File(config.embeddingPath).exists()) {
            throw new ConfigurationException("Embeddings path does not exist or is empty");
        }
        WordVectorExtractor wve = new WordVectorExtractor(config.embeddingPath);
        WordAligner aligner = new CosineSimAligner(wve);
        if (config.saliencyFile != null && !new File(config.saliencyFile).exists()) {
            throw new ConfigurationException("Saliency file does not exist");
        }
        SaliencyFeatures saliencyFeatures = config.saliencyFile == null ? null : new SaliencyFeatures(new File(config.saliencyFile));
        if (config.features == null) {
            throw new ConfigurationException("Features can't be null");
        }
        StandardWordAlignmentFeatureExtractor wafe = new StandardWordAlignmentFeatureExtractor(config.features == null ? Arrays.asList(DEFAULT_FEATURES) : config.features, saliencyFeatures);

        return new DefaultFeatureExtractor(aligner, wafe, config.stopwords, tags);
    }

    /**
     * The standard feature extraction from word alignment
     *
     * @author John McCrae
     */
    public class DefaultFeatureExtractor implements TextFeature {

        private final WordAligner wordAligner;
        private final StandardWordAlignmentFeatureExtractor wafe;
        private final String[] featureNames;
        private final Set<String> stopwords;
        private final Set<String> tag;

        public DefaultFeatureExtractor(WordAligner wordAligners,
                StandardWordAlignmentFeatureExtractor wafe,
                String stopwordsFile,
                Set<String> tag) {
            this.wordAligner = wordAligners;
            this.wafe = wafe;
            this.tag = tag;
            ArrayList<String> _featureNames = new ArrayList<>();
            String[] _names = Arrays.copyOf(wafe.featureNames(), wafe.featureNames().length);
            for (int i = 0; i < _names.length; i++) {
                _names[i] = _names[i] + "-" + wordAligner.id();
            }
            _featureNames.addAll(Arrays.asList(_names));
            featureNames = _featureNames.toArray(new String[_featureNames.size()]);
            if (stopwordsFile != null) {
                stopwords = new HashSet<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(stopwordsFile))) {
                    String word;
                    while ((word = reader.readLine()) != null) {
                        stopwords.add(word);
                    }
                } catch (IOException x) {
                    throw new RuntimeException("Could not read stopwords @ " + stopwordsFile, x);
                }
            } else {
                stopwords = null;
            }
        }

        @Override
        public String id() {
            return "word-align";
        }

        @Override
        public Set<String> tags() {
            return tag;
        }

        @Override
        public Feature[] extractFeatures(LensResult facet, NaiscListener log) {
            double[] d = new double[featureNames.length];
            if (facet.lang1.equals(facet.lang2)) {
                d = _extractFeatures(facet.string1, facet.string2, wordAligner);
                return Feature.mkArray(d,featureNames);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public String[] getFeatureNames() {
            return featureNames;
        }

        private double[] _extractFeatures(String s1, String s2, WordAligner a) {
            if (stopwords != null) {
                return wafe.makeFeatures(a.align(s1, s2, stopwords));
            } else {
                return wafe.makeFeatures(a.align(s1, s2));
            }

        }

        @Override
        public void close() throws IOException {
        }
    }

}
