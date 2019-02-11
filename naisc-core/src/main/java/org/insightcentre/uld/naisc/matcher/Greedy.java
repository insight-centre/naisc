package org.insightcentre.uld.naisc.matcher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.insightcentre.uld.naisc.constraint.UnsolvableConstraint;
import org.insightcentre.uld.naisc.constraint.Constraint;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Map;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.MatcherFactory;
import org.insightcentre.uld.naisc.main.Configuration.ConstraintConfiguration;
import org.insightcentre.uld.naisc.main.ConfigurationException;

/**
 * Solve the alignment problem greedily
 * 
 * @author John McCrae
 */
public class Greedy implements MatcherFactory {

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

    }


    @Override
    public Matcher makeMatcher(Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if(config.constraint == null)
            throw new ConfigurationException("Greedy matcher requires a constraint");
        
        return new GreedySearch(config.threshold, config.constraint.make());
    }

    @Override
    public String id() {
        return "greedy";
    }
    
    private static class GreedySearch implements Matcher {

        private final double threshold;
        private Constraint constraint;

        public GreedySearch(double threshold, Constraint initialScore) {
            this.threshold = threshold;
            this.constraint = initialScore;
        }

        @Override
        public AlignmentSet align(AlignmentSet matches) {
            Constraint lastComplete = constraint.complete() ? constraint : null;
            matches.sortAlignments();
            for(Alignment alignment : matches.getAlignments()) {
                if(alignment.score >= threshold && constraint.canAdd(alignment)) {
                    Constraint newConstraint = constraint.add(alignment);
                    if(newConstraint.score > constraint.score) 
                        constraint = newConstraint;
                    if(lastComplete == null || newConstraint.complete() && newConstraint.score > lastComplete.score)
                        lastComplete = newConstraint;
                }
            }
            if(lastComplete != null)
                return new AlignmentSet(lastComplete.alignments(new ArrayList<>()));
            else
                throw new UnsolvableConstraint("No complete solution was generated");
        }

    }

}
