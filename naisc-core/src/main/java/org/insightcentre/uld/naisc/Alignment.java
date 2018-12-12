package org.insightcentre.uld.naisc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.apache.jena.rdf.model.Resource;

/**
 * An alignment between two entities
 * 
 * @author John McCrae 
 */
public class Alignment {
    /** The first entity that is aligned */
    public final String entity1;
    /** The second entity that is aligned */
    public final String entity2;
    /** The score (between 0 and 1) of the alignment */
    public final double score;
    /** The alignment type */
    public final String relation;
    
    public static final String SKOS_EXACT_MATCH = "http://www.w3.org/2004/02/skos/core#exactMatch";

    public Alignment(Resource entity1, 
        Resource entity2, 
        double score) {
        this.entity1 = entity1.getURI();
        this.entity2 = entity2.getURI();
        this.score = score;
        this.relation = SKOS_EXACT_MATCH;
        assert(score >= 0 && score <= 1);
    }

       public Alignment(Resource entity1, 
        Resource entity2, 
        double score, String relation) {
        this.entity1 = entity1.getURI();
        this.entity2 = entity2.getURI();
        this.score = score;
        this.relation = relation;
        assert(score >= 0 && score <= 1);
    } 

    @JsonCreator public Alignment(@JsonProperty("entity1") String entity1, 
        @JsonProperty("entity2") String entity2, 
        @JsonProperty("score") double score,
        @JsonProperty("relation") String relation) {
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.score = score;
        this.relation = relation;
        assert(score >= 0 && score <= 1);
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
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.entity1);
        hash = 29 * hash + Objects.hashCode(this.entity2);
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
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
        final Alignment other = (Alignment) obj;
        if (!Objects.equals(this.entity1, other.entity1)) {
            return false;
        }
        if (!Objects.equals(this.entity2, other.entity2)) {
            return false;
        }
        if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
            return false;
        }
        return true;
    }

    
    
}
