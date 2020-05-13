package org.insightcentre.uld.naisc.constraint;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.ConfigurationParameter;

/**
 * Constraint that accepts any solution where values are above a threshold. With
 * no parameters this constraint can be used to accept all solutions.
 *
 * @author John McCrae
 */
public class ThresholdConstraint implements ConstraintFactory {

    /**
     * The configuration for the threshold constraint.
     */
    public static class Configuration {
        /** The minimum threshold to accept. */
        @ConfigurationParameter(description="The minimum threshold to accept")
        public double threshold = Double.NEGATIVE_INFINITY;
    }

    @Override
    public Constraint make(Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        return new ThresholdImpl(new ArrayList<>(), 0.0, config.threshold);
    }

    private static class ThresholdImpl extends Constraint {

        private final double threshold;
        private final List<Alignment> alignments;

        public ThresholdImpl(List<Alignment> alignments, double score, double threshold) {
            super(score);
            this.threshold = threshold;
            this.alignments = alignments;
        }

        @Override
        public void add(Alignment alignment) {
            score += delta(alignment);
            alignments.add(alignment);
        }

        @Override
        public boolean canAdd(Alignment alignment) {
            return alignment.probability >= threshold;
        }

        @Override
        public List<Alignment> alignments() {
            return alignments;
        }

        @Override
        public Constraint copy() {
            List<Alignment> newAligns = new ArrayList<>(alignments);
            return new ThresholdImpl(newAligns, score, threshold);
        }
    }

}
