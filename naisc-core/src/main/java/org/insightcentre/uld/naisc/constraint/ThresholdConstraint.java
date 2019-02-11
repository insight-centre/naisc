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
        return new ThresholdImpl(null, null, 0.0, config.threshold);
    }

    private static class ThresholdImpl extends Constraint {

        private final double threshold;
        private final Alignment alignment;
        private final ThresholdImpl parent;

        public ThresholdImpl(Alignment alignment, ThresholdImpl parent, double score, double threshold) {
            super(score);
            this.threshold = threshold;
            this.alignment = alignment;
            this.parent = parent;
        }

        @Override
        public Constraint add(Alignment alignment) {
            double newScore = score + delta(alignment);
            return new ThresholdImpl(alignment, this, newScore, threshold);
        }

        @Override
        public boolean canAdd(Alignment alignment) {
            return alignment.score >= threshold;
        }

        @Override
        public List<Alignment> alignments(List<Alignment> alignments) {
            if(alignment != null) {
                alignments.add(alignment);
                return parent.alignments(alignments);
            } else {
                return alignments;
            }
        }
        
        

    }

}
