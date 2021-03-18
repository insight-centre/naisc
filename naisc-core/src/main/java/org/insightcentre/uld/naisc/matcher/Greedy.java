package org.insightcentre.uld.naisc.matcher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.constraint.UnsolvableConstraint;
import org.insightcentre.uld.naisc.constraint.Constraint;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.insightcentre.uld.naisc.main.Configuration.ConstraintConfiguration;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.main.ExecuteListener;

/**
 * Solve the alignment problem greedily
 *
 * @author John McCrae
 */
public class Greedy implements MatcherFactory {

    /**
     * The configuration of the greedy matcher.
     */
     @ConfigurationClass("The greedy matcher finds a solution given an arbitrary constraint quickly by always taking the highest scoring link. It may produce poorer results than other methods")
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

    }

    @Override
    public Matcher makeMatcher(Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if (config.constraint == null) {
            throw new ConfigurationException("Greedy matcher requires a constraint");
        }

        return new GreedySearch(config.threshold, config.constraint.make());
    }

    @Override
    public String id() {
        return "greedy";
    }

    private static class GreedySearch implements Matcher {

        private final double threshold;
        private Constraint initialScore;

        public GreedySearch(double threshold, Constraint initialScore) {
            this.threshold = threshold;
            this.initialScore = initialScore;
        }

        @Override
        public AlignmentSet alignWith(AlignmentSet matches, AlignmentSet initial, ExecuteListener listener) {
            Constraint constraint = initialScore;
            for (Alignment init : initial) {
                if (!constraint.canAdd(init)) {
                    listener.updateStatus(ExecuteListener.Stage.MATCHING, "A link from the initial set is not valid with the constraint.");
                }
                //constraint = constraint.add(init);
                constraint.add(init);
            }
            Constraint lastComplete = constraint.complete() ? constraint : null;
            matches.sortAlignments();
            if (matches.getAlignments().isEmpty()) {
                listener.updateStatus(NaiscListener.Stage.MATCHING, "No alignments generated");
            }
            int overThreshold = 0;
            int nonFinite = 0;
            PrintWriter out;
            try {
                out = new PrintWriter("tmp.data");
            } catch(IOException x) { out = null; }
            for (Alignment alignment : matches.getAlignments()) {
                if(out != null) out.printf("%s,%s,%.4f,", alignment.entity1, alignment.entity2, alignment.probability);
                if (alignment.probability > threshold && constraint.canAdd(alignment)) {
                    overThreshold++;
                    //Constraint newConstraint = constraint.add(alignment);
                    double newScore = constraint.score + constraint.delta(alignment);
                    if (newScore > constraint.score || (newScore == constraint.score && threshold < 0)) {
                        //constraint = newConstraint;
                        constraint.add(alignment);
                        if(out != null) out.println("TRUE");
                    }  else {
                        if(out != null) out.println("NOGAIN");
                    }
                    if (lastComplete == null || constraint.canComplete(alignment) && newScore > lastComplete.score) {
                        lastComplete = constraint.copy();
                        lastComplete.add(alignment);
                    }
                } else if(!Double.isFinite(alignment.probability)) {
                    nonFinite++;
                    if(out != null) out.println("NONFINITE");
                } else {
                    if(out != null) out.println("CONSTRAINT");
                }
            }

            if(out != null) out.close();
            if (lastComplete != null) {
                List<Alignment> alignment = lastComplete.alignments();
                listener.updateStatus(NaiscListener.Stage.MATCHING, String.format("Predicted %d/%d alignments (%d non-finite, probability=%.4f)", alignment.size(), overThreshold, nonFinite, lastComplete.score));
                return new AlignmentSet(alignment);
            } else {
                throw new UnsolvableConstraint("No complete solution was generated");
            }
        }

    }

}
