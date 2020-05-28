package org.insightcentre.uld.naisc.matcher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.MatcherFactory;
import org.insightcentre.uld.naisc.constraint.Constraint;
import org.insightcentre.uld.naisc.constraint.UnsolvableConstraint;
import org.insightcentre.uld.naisc.main.Configuration.ConstraintConfiguration;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.main.ExecuteListener;
import org.insightcentre.uld.naisc.util.Beam;

/**
 * A search algorithm for finding near optimal solutions for generic
 * constraints. The algorithm keeps a 'beam' of partial solutions and tries each
 * new solution in order to guarantee the solution quality. The alignments are
 * added in descending order by probability and so some constrains may not be solvable
 * with this algorithm.
 *
 * @author John McCrae
 */
public class BeamSearch implements MatcherFactory {

    private static final int DEFAULT_BEAM_SIZE = 100;

    @Override
    public String id() {
        return "beam-search";
    }

    @Override
    public Matcher makeMatcher(Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if (config.constraint == null) {
            throw new ConfigurationException("Greedy matcher requires a constraint");
        }
        if (config.beamSize <= 0) {
            config.beamSize = DEFAULT_BEAM_SIZE;
        }
        if (config.maxIterations < 0) {
            config.maxIterations = 0;
        }
        return new BeamSearchImpl(config.threshold, config.constraint.make(), config.beamSize, config.maxIterations);
    }

    /**
     * The configuration of the greedy matcher.
     */
    public static class Configuration {

        /**
         * The constraint that the searcher will optimize
         */
        @ConfigurationParameter(description = "The constraint that the searcher will optimize")
        public ConstraintConfiguration constraint;
        /**
         * The threshold (minimum value to accept)
         */
        @ConfigurationParameter(description = "The threshold (minimum value to accept)")
        public double threshold = Double.NEGATIVE_INFINITY;
        /**
         * The size of beam. Trades the speed and memory usage of the algorithm
         * off with the quality of the solution
         */
        @ConfigurationParameter(description = "The size of beam. Trades the speed and memory usage of the algorithm off with the quality of the solution")
        public int beamSize = DEFAULT_BEAM_SIZE;

        /**
         * The maximum number of iterations to perform (zero for no limit)
         */
        @ConfigurationParameter(description = "The maxiumum number of iterations to perform (zero for no limit)")
        public int maxIterations = 0;

    }

    private static class BeamSearchImpl implements Matcher {

        private final double threshold;
        private final Constraint initialScore;
        private final int beamSize;
        private final int maxIterations;

        public BeamSearchImpl(double threshold, Constraint constraint, int beamSize, int maxIterations) {
            this.threshold = threshold;
            this.initialScore = constraint;
            this.beamSize = beamSize;
            this.maxIterations = maxIterations;
        }

        @Override
        public AlignmentSet alignWith(AlignmentSet matches, AlignmentSet initial, ExecuteListener listener) {
            matches.sortAlignments();
            Constraint score = initialScore.copy();
            for(Alignment init : initial) {
                if(!score.canAdd(init)) {
                    listener.updateStatus(ExecuteListener.Stage.MATCHING, "A link from the initial set is not valid with the constraint.");
                }
                score.add(init);
            }
            Beam<Constraint> beam = new Beam<>(beamSize);
            Constraint bestValid = initialScore.complete() ? initialScore : null;
            beam.insert(score, score.score);

            int iter = 0;
            for (Alignment alignment : matches.getAlignments()) {
                if (alignment.probability >= threshold) {
                    for (Constraint current : beam) {
                        if (current.canAdd(alignment)) {
                            Constraint ts = current.copy();
                            ts.add(alignment);
                            beam.insert(ts, ts.score);
                            if (ts.complete() && (bestValid == null || bestValid.score < ts.score)) {
                                bestValid = ts.copy();
                            }
                        }
                    }
                }
                if (++iter > maxIterations && maxIterations > 0) {
                    break;
                }
                if(iter % 10000 == 0 && listener != null) {
                    listener.updateStatus(ExecuteListener.Stage.MATCHING, 
                            String.format("Generated %sth candidate (max probability=%.2f)", iter,
                                    bestValid == null ? 0.0 : bestValid.score));
                }
            }
            if (bestValid != null) {
                return new AlignmentSet(bestValid.alignments());
            } else {
                throw new UnsolvableConstraint("Beam search did not find any complete solutions");
            }
        }

    }

}
