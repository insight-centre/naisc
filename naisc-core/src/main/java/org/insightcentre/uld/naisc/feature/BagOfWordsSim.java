package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.PrettyGoodTokenizer;
import org.insightcentre.uld.naisc.TextFeature;
import org.insightcentre.uld.naisc.TextFeatureFactory;

/**
 * Compare the bag-of-words similarity of a bunch of features. This is often quite
 * a useful and very quick baseline to compute.
 * 
 * @author John McCrae
 */
public class BagOfWordsSim implements TextFeatureFactory {
    /** The similarity method to use */
    public static enum SimMethod {
        /** Use Jaccard similarity */
        jaccard,
        /** Use Exponentially weighted Jaccard */
        jaccardExponential
    }

    /** Configuration for bag of words similarity */
    public static class Configuration {
        /** The similarity method to use */
        @ConfigurationParameter(description="The similarity method to use")
        public SimMethod method = SimMethod.jaccard;
        /** The weighting function. Near-zero values will penalize low agreement more
         * while high values will be nearly binary
         */
        @ConfigurationParameter(description = "The weighting value. Near-zero values will penalize low agreement morewhile high values will be nearly binary")
        public double weighting = 1.0;
    }


    @Override
    public TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        return new BagOfWordsFeatureExtractor(config.method, config.weighting, tags);
    }

    private static class BagOfWordsFeatureExtractor implements TextFeature {

        private final SimMethod method;
        private final double weighting;
        private final Set<String> tags;

        public BagOfWordsFeatureExtractor(SimMethod method, double weighting, Set<String> tags) {
            assert(method != null);
            this.method = method;
            this.weighting = weighting;
            this.tags = tags;
        }

        @Override
        public String id() {
            return "bag-of-words";
        }

        private double sigma(int x) {
            return 1.0 - Math.exp(-weighting * x);
        }

        @Override
        public double[] extractFeatures(LangStringPair lsp, NaiscListener log) {
            Set<String> w1 = new HashSet<>(Arrays.asList(PrettyGoodTokenizer.tokenize(lsp._1)));
            int a = w1.size();
            Set<String> w2 = new HashSet<>(Arrays.asList(PrettyGoodTokenizer.tokenize(lsp._2)));
            int b = w2.size();
            w1.retainAll(w2);
            int ab = w1.size();

            switch (method) {
                case jaccard:
                    return new double[]{(double) ab / (double) (a + b - ab)};
                case jaccardExponential:
                    return new double[]{sigma(ab) / (sigma(a) + sigma(b) - sigma(ab))};
                default:
                    throw new RuntimeException("Unreachable");

            }
        }

        @Override
        public String[] getFeatureNames() {
            return new String[]{"bow"};
        }

        @Override
        public Set<String> tags() {
            return tags;
        }


        @Override
        public void close() throws IOException {
        }

    }

}
