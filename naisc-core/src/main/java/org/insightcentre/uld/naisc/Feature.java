package org.insightcentre.uld.naisc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Feature {
    public final String name;
    public final double value;

    @JsonCreator
    public Feature(@JsonProperty("name") String name, @JsonProperty("value") double value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    /**
     * Convert an array of values with names to a feature array
     */
    public static Feature[] mkArray(double[] values, String[] names) {
        if(values.length != names.length) { throw new IllegalArgumentException("Wrong length of arrays"); }
        Feature[] f = new Feature[values.length];
        for(int i = 0; i < values.length; i++) {
            f[i] = new Feature(names[i], values[i]);
        }
        return f;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Feature feature = (Feature) o;

        if (Double.compare(feature.value, value) != 0) return false;
        return name.equals(feature.name);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name.hashCode();
        temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Feature{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
