package org.insightcentre.uld.naisc.matcher;

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
import org.insightcentre.uld.naisc.util.Beam;

/**
 * A search algorithm for finding near optimal solutions for generic constraints.
 * The algorithm keeps a 'beam' of partial solutions and tries each new solution
 * in order to guarantee the solution quality. The alignments are added in descending
 * order by score and so some constrains may not be solvable with this algorithm.
 * 
 * @author John McCrae
 */
public class BeamSearch implements MatcherFactory {

    @Override
    public String id() {
        return "beam-search";
    }

    @Override
    public Matcher makeMatcher(Map<String, Object> params) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if (config.constraint == null) {
            throw new ConfigurationException("Greedy matcher requires a constraint");
        }
        return new BeamSearchImpl(config.threshold, config.constraint.make(), config.beamSize);
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
        public final int beamSize = 1000;

    }

    private static class BeamSearchImpl implements Matcher {

        private final double threshold;
        private final Constraint initialScore;
        private final int beamSize;

        public BeamSearchImpl(double threshold, Constraint constraint, int beamSize) {
            this.threshold = threshold;
            this.initialScore = constraint;
            this.beamSize = beamSize;
        }

        @Override
        public AlignmentSet align(AlignmentSet matches) {
            matches.sortAlignments();
            Constraint score = initialScore;
            Beam<Constraint> beam = new Beam<>(beamSize);
            Constraint bestValid = initialScore.complete() ? initialScore : null;
            beam.insert(score, score.score);

            for (Alignment alignment : matches.getAlignments()) {
                if (alignment.score >= threshold) {
                    for (Constraint current : beam) {
                        if (current.canAdd(alignment)) {
                            Constraint ts = current.add(alignment);
                            beam.insert(ts, ts.score);
                            if(bestValid == null || ts.complete() && bestValid.score < ts.score) {
                                bestValid = ts;
                            }
                        }
                    }
                }
            }
            if(bestValid != null) {
                return new AlignmentSet(bestValid.alignments);
            } else {
                throw new UnsolvableConstraint("Beam search did not find any complete solutions");
            }
        }

    }

}
