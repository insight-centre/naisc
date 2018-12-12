package org.insightcentre.uld.naisc.scorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static java.lang.Double.max;
import static java.lang.Double.min;
import java.util.Map;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.ScorerFactory;
import org.insightcentre.uld.naisc.ScorerTrainer;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;

/**
 * A scorer that averages the features given as input. Please note that this
 * score is limited to the range [0,1] and so may produce odd results
 * 
 * @author John McCrae
 */
public class Average implements ScorerFactory {
    private ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String id() {
        return "average";
    }

    @Override
    public Scorer makeScorer(Map<String, Object> params) {
        Configuration config = mapper.convertValue(params, Configuration.class);
        return new AverageImpl(config.weights, config.property, config.softmax);
    }

    @Override
    public Option<ScorerTrainer> makeTrainer(Map<String, Object> params) {
        return new None<>();
    }
    
    /**
     * The configuration of the averaging scorer
     */
    public static class Configuration {
        /**
         * The weights to be applied to the features. Or null for all features as 1.0
         */
        @ConfigurationParameter(description = "The weights to be applied to the features")
        public double[] weights;
        /**
         * The relation to predict.
         */
        @ConfigurationParameter(description = "The relation to predict")
        public String property;
        /**
         * Apply a soft clipping of average using the sigmoid function. If false
         * the method simply outputs 0 for all negative scores and 1 for all greater
         * than one.
         */
        @ConfigurationParameter(description = "Apply a soft clipping of average using the sigmoid function")
        public boolean softmax = true;
    }
    
    private static class AverageImpl implements Scorer {
        private double[] weights;
        private String relation;
        private boolean softmax;

        public AverageImpl(double[] weights, String relation, boolean softmax) {
            this.weights = weights;
            this.relation = relation;
            this.softmax = softmax;
        }
        

        @Override
        public double similarity(FeatureSet features) {
            if(weights != null && weights.length != features.values.length) {
                throw new IllegalArgumentException("Length of feature vector does not match that of weights");
            }
            double score = 0.0;
            for(int i = 0; i < features.values.length; i++) {
                if(weights == null) {
                    score += features.values[i] / features.values.length;
                } else {
                    score += features.values[i] * weights[i];
                }
            }
            if(softmax) {
                return sigmoid(score);
            } else {
                return max(0.0, min(1.0, score));
            }
        }

        @Override
        public String relation() {
            return relation;
        }

        @Override
        public void close() throws IOException {
        }
        
        
    }
    
    private static double sigmoid(double d) {
        return 1.0/ (1.0 + Math.exp(-d));
    }

}
