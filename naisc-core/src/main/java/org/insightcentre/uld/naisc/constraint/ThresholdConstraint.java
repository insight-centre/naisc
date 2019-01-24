package org.insightcentre.uld.naisc.constraint;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
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

        public ThresholdImpl(List<Alignment> alignments, double score, double threshold) {
            super(alignments, score);
            this.threshold = threshold;
        }

        @Override
        public Constraint add(Alignment alignment) {
            List<Alignment> newAligns = new ArrayList<>(alignments);
            newAligns.add(alignment);
            double newScore = score + delta(alignment);
            return new ThresholdImpl(newAligns, newScore, threshold);
        }

        @Override
        public boolean canAdd(Alignment alignment) {
            return alignment.score >= threshold;
        }

    }

}
