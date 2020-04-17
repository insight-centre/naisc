package org.insightcentre.uld.naisc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.insightcentre.uld.naisc.util.StringPair;

/**
 * The feature set with a similarity probability (for training)
 * @author John McCrae
 */
public class FeatureSetWithScore extends FeatureSet {
   public final double score;

   @JsonCreator public FeatureSetWithScore(
       @JsonProperty("probability") double score,
       @JsonProperty("names") StringPair[] names, 
       @JsonProperty("values") double[] values, 
       @JsonProperty("entity1") String entity1, 
       @JsonProperty("entity2") String entity2) {
        super(names, values);
        this.score = score;
    }
   
    private ObjectMapper mapper;

    @Override
    public String toString() {
        if(mapper == null) {
            mapper = new ObjectMapper();
        }
        try {
            return mapper.writeValueAsString(this);
        } catch(JsonProcessingException x) {
            throw new RuntimeException(x);
        }
    }


    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FeatureSetWithScore other = (FeatureSetWithScore) obj;
        if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
            return false;
        }
        return super.equals(obj);
    }
   
    
}
