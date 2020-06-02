package org.insightcentre.uld.naisc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Tbe result of scoring
 * @author John McCrae
 */
public class ScoreResult {
    private final double value;
    private final String relation;

    /**
     * Create a new score result
     * @param value The probability between 0 and 1
     * @param relation The relationship being predicted
     */
     @JsonCreator
    public ScoreResult(@JsonProperty("probability") double value, @JsonProperty("property") String relation) {
        this.value = value;
        this.relation = relation;
    }

    /**
     * Get the result of scoring
     * @return A probability value
     */
    public double getProbability() { return value; }

    /**
     * Get the property that is predicted by this scorer
     * @return The URI of the property to be predicted
     */
    public String getProperty() { return relation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoreResult that = (ScoreResult) o;
        return Double.compare(that.value, value) == 0 &&
                Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, relation);
    }
}
