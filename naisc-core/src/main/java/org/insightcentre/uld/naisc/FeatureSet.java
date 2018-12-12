package org.insightcentre.uld.naisc;

import org.insightcentre.uld.naisc.util.Pair;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.util.StringPair;

/**
 * A feature for predicting similarity
 * 
 * @author John McCrae
 */
public class FeatureSet {
   public final StringPair[] names;
   public final double[] values;
   public final String entity1, entity2;

   /**
    * Create an empty feature set
    * @param entity1 The first entity
    * @param entity2 The second entity
    */
    public FeatureSet(Resource entity1, Resource entity2) {
        this.names = new StringPair[] {};
        this.values = new double[] {};
        this.entity1 = entity1.getURI();
        this.entity2 = entity2.getURI();
    }
   /**
    * Create an empty feature set
    * @param entity1id The first entity's ID
    * @param entity2id The second entity's ID
    */
    public FeatureSet(String entity1id, String entity2id) {
        this.names = new StringPair[] {};
        this.values = new double[] {};
        this.entity1 = entity1id;
        this.entity2 = entity2id;
    }
    
    @JsonCreator public FeatureSet(@JsonProperty("name") StringPair[] names, 
        @JsonProperty("values") double[] values, 
        @JsonProperty("entity1") String entity1, 
        @JsonProperty("entity2") String entity2) {
        this.names = names == null ? new StringPair[] {} : names;
        this.values = values == null ? new double[] {} : values;
        this.entity1 = entity1;
        this.entity2 = entity2;
        assert(this.names.length == this.values.length);
        assert(entity1 != null);
        assert(entity2 != null);
    }
    
    public FeatureSet(String[] featureNames, String lensName,
        double[] values, Resource entity1, Resource entity2) {
        assert(featureNames != null);
        this.names = new StringPair[featureNames.length];
        for(int i = 0; i < featureNames.length; i++) {
            this.names[i] = new StringPair(featureNames[i], lensName);
        }
        this.values = values == null ? new double[] {} : values;
        assert(entity1 != null);
        assert(entity2 != null);
        this.entity1 = entity1.getURI();
        this.entity2 = entity2.getURI();
        assert(this.names.length == this.values.length);
    }

//    /**
//     * Add this dataset to another adding a string to the names of each element. 
//     * This should be used when combining two similar feature sets from different
//     * sources
//     * @param other The feature set to combine
//     * @param thisName The prefix to add to this 
//     * @param otherName The prefix to add to the other
//     * @return  The combination of the two datasets
//     */
//    public FeatureSet addAliased(FeatureSet other, String thisName, String otherName) {
//        assert(other.entity1.equals(entity1));
//        assert(other.entity2.equals(entity2));
//        // Names must be unique for WEKA!
//        String[] names2 = new String[names.length + other.names.length];
//        for(int i = 0; i < names.length; i++) {
//            names2[i] = names[i] + thisName;
//        }
//        for(int i = 0; i < other.names.length; i++) {
//            names2[i + names.length] = other.names[i] + otherName;
//        }
//        double[] values2 = new double[values.length + other.values.length];
//        System.arraycopy(values, 0, values2, 0, values.length);
//        System.arraycopy(other.values, 0, values2, values.length, other.values.length);
//        return new FeatureSet(names2, values2, entity1, entity2);
//
//    }


    
    /**
     * Add two feature sets together by concatenating features. Does not change
     * this dataset!
     * @param other The dataset to add
     * @return The combined result of these datasets
     */
    public FeatureSet add(FeatureSet other) {
        assert(other.entity1.equals(entity1));
        assert(other.entity2.equals(entity2));
        // Names must be unique for WEKA!
        Set<StringPair> nameSet = new HashSet<>(Arrays.asList(names));
        for(int i = 0; i < other.names.length; i++) {
            if(nameSet.contains(other.names[i])) {
                throw new IllegalArgumentException("Adding features with duplicate names");
            } else {
                nameSet.add(other.names[i]);
            }
        }
        StringPair[] names2 = new StringPair[names.length + other.names.length];
        System.arraycopy(names, 0, names2, 0, names.length);
        System.arraycopy(other.names, 0, names2, names.length, other.names.length);
        double[] values2 = new double[values.length + other.values.length];
        System.arraycopy(values, 0, values2, 0, values.length);
        System.arraycopy(other.values, 0, values2, values.length, other.values.length);
        return new FeatureSet(names2, values2, entity1, entity2);
    }

    public FeatureSetWithScore withScore(double score) {
        return new FeatureSetWithScore(score, names, values, entity1, entity2);
    }
    
    public boolean hasNonMissing() {
        for(int i = 0; i < values.length; i++) {
            if(!Double.isNaN(values[i]))
                return true;
        }
        return false;
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
        int hash = 3;
        hash = 23 * hash + Arrays.deepHashCode(this.names);
        hash = 23 * hash + Arrays.hashCode(this.values);
        hash = 23 * hash + Objects.hashCode(this.entity1);
        hash = 23 * hash + Objects.hashCode(this.entity2);
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
        final FeatureSet other = (FeatureSet) obj;
        if (!Arrays.deepEquals(this.names, other.names)) {
            return false;
        }
        if (!Arrays.equals(this.values, other.values)) {
            return false;
        }
        if (!Objects.equals(this.entity1, other.entity1)) {
            return false;
        }
        if (!Objects.equals(this.entity2, other.entity2)) {
            return false;
        }
        return true;
    }
   
}
