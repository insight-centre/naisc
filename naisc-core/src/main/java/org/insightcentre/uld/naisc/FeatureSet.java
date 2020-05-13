package org.insightcentre.uld.naisc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import it.unimi.dsi.fastutil.objects.*;
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

   /**
    * Create an empty feature set
    */
    public FeatureSet() {
        this.names = new StringPair[] {};
        this.values = new double[] {};
    }

    @JsonCreator public FeatureSet(@JsonProperty("name") StringPair[] names, 
        @JsonProperty("values") double[] values) {
        this.names = names == null ? new StringPair[] {} : names;
        this.values = values == null ? new double[] {} : values;
        assert(this.names.length == this.values.length);
    }
    
    public FeatureSet(String[] featureNames, String lensName,
        double[] values) {
        assert(featureNames != null);
        this.names = new StringPair[featureNames.length];
        for(int i = 0; i < featureNames.length; i++) {
            this.names[i] = new StringPair(featureNames[i], lensName);
        }
        this.values = values == null ? new double[] {} : values;
        assert(this.names.length == this.values.length);
    }

    public FeatureSet(Feature[] features, String lensName) {
        this.names = new StringPair[features.length];
        this.values = new double[features.length];
        int i = 0;
        for(Feature f : features) {
            this.names[i] = new StringPair(features[i].name, lensName);
            this.values[i] = features[i].value;
            i++;
        }
    }

    public FeatureSet(List<Feature> features) {
        List<StringPair> fnames = new ArrayList<>();
        Object2IntMap<String> freq = new Object2IntOpenHashMap<>();
        this.values = new double[features.size()];
        int i = 0;
        for(Feature f : features) {
            fnames.add(new StringPair(f.name, "" + freq.getOrDefault(f.name, 1)));
            freq.put(f.name, freq.getOrDefault(f.name, 1) + 1);
            values[i++] = f.value;
        }
        this.names = fnames.toArray(new StringPair[fnames.size()]);
    }

    /**
     * Add two feature sets together by concatenating features. Does not change
     * this dataset!
     * @param other The dataset to add
     * @return The combined result of these datasets
     */
    public FeatureSet add(FeatureSet other) {
        // Names must be unique for WEKA!
        Set<StringPair> nameSet = new HashSet<>(Arrays.asList(names));
        for(int i = 0; i < other.names.length; i++) {
            if(nameSet.contains(other.names[i])) {
                throw new IllegalArgumentException("Adding features with duplicate names: " + other.names[i]);
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
        return new FeatureSet(names2, values2);
    }

    public FeatureSetWithScore withScore(double score) {
        return new FeatureSetWithScore(score, names, values, "", "");
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
        return true;
    }

    public boolean isEmpty() {
        return names.length == 0;
    }
   
}
