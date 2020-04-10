package org.insightcentre.uld.naisc.matcher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Map;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.MatcherFactory;
import org.insightcentre.uld.naisc.main.ExecuteListener;

/**
 * Simply extract all links whose probability is over a given threshold
 * 
 * @author John McCrae
 */
public class Threshold implements MatcherFactory {
    private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    @Override
    public String id() {
        return "threshold";
    }

    @Override
    public Matcher makeMatcher(Map<String, Object> params) {
        Configuration config = mapper.convertValue(params, Configuration.class);
        return new ThresholdImpl(config.threshold);
    }

    /**
     * The configuration for a threshold matcher.
     */
    public static class Configuration {
        /**
         * The threshold to accept.
         */
        @ConfigurationParameter(description = "The threshold to accept")
        public double threshold = 0.5;
    }
    
    private static class ThresholdImpl implements Matcher {
        private final double threshold;

        public ThresholdImpl(double threshold) {
            this.threshold = threshold;
        }
        
        @Override
        public AlignmentSet alignWith(AlignmentSet matches, AlignmentSet initial, ExecuteListener monitor) {
            matches.addAll(initial);
            Iterator<Alignment> iter = matches.iterator();
            while(iter.hasNext()) {
                final double score = iter.next().probability;
                if(score < threshold)
                    iter.remove();
            }
            
            return matches;
        }

        
    }
}
