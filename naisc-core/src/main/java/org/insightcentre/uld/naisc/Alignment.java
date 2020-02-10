package org.insightcentre.uld.naisc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * An alignment between two entities
 *
 * @author John McCrae
 */
public class Alignment {

    /**
     * The first entity that is aligned
     */
     @JsonSerialize(using=ResourceSerializer.class)
    public final Resource entity1;
    /**
     * The second entity that is aligned
     */
    @JsonSerialize(using=ResourceSerializer.class)
    public final Resource entity2;
    /**
     * The score (between 0 and 1) of the alignment
     */
    public final double score;
    /**
     * The alignment type
     */
    public final String relation;
    /**
     * The evaluation mark for this alignment
     */
    public Valid valid = Valid.unknown;
    /**
     * The features used to calculate the score
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Object2DoubleMap<String> features;

    public static final String SKOS_EXACT_MATCH = "http://www.w3.org/2004/02/skos/core#exactMatch";

    public Alignment(Resource entity1,
            Resource entity2,
            double score) {
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.score = score;
        this.relation = SKOS_EXACT_MATCH;
        assert (score >= 0 && score <= 1);
    }

    public Alignment(Resource entity1,
            Resource entity2,
            double score, String relation, Object2DoubleMap<String> features) {
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.score = score;
        this.relation = relation;
        assert (score >= 0 && score <= 1);
        this.features = features;
    }
    
    public Alignment(Alignment a, double score, Valid valid) {
        this.entity1 = a.entity1;
        this.entity2 = a.entity2;
        this.score = score;
        this.relation = a.relation;
        this.valid = valid;
        assert (score >= 0 && score <= 1);
    }


    public Alignment(Statement statement, double score) {
        assert(statement.getSubject().isURIResource() && statement.getObject().isURIResource());
        this.entity1 = statement.getSubject();
        this.entity2 = statement.getObject().asResource();
        this.score = score;
        this.relation = statement.getPredicate().getURI();
        assert (score >= 0 && score <= 1);
    }
    

    @Override
    public String toString() {
        return "Alignment{" + "entity1=" + entity1 + ", entity2=" + entity2 + ", score=" + score + ", relation=" + relation + ", valid=" + valid + '}';
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

    /**
     * Values for validity of the alignment
     */
    public enum Valid {
        /**
         * Marked as correct
         */
        yes,
        /**
         * Marked as incorrect
         */
        no,
        /**
         * Not marked
         */
        unknown,
        /**
         * Correct but not generated by this system
         */
        novel
    }

    private static class ResourceSerializer extends JsonSerializer<Resource> {

        @Override
        public void serialize(Resource resource, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if(resource.isURIResource()) {
                jsonGenerator.writeString(resource.getURI());
            } else {
                jsonGenerator.writeString("_:" + resource.getId().getLabelString());
            }
        }
    }
}
