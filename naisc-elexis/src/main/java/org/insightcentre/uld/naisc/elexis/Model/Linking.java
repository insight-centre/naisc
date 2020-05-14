package org.insightcentre.uld.naisc.elexis.Model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "source_sense",
        "target_sense",
        "type",
        "score"
})
public class Linking {

    @JsonProperty("source_sense")
    private String sourceSense;
    @JsonProperty("target_sense")
    private String targetSense;
    @JsonProperty("type")
    private String type;
    @JsonProperty("score")
    private Double score;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("source_sense")
    public String getSourceSense() {
        return sourceSense;
    }

    @JsonProperty("source_sense")
    public void setSourceSense(String sourceSense) {
        this.sourceSense = sourceSense;
    }

    @JsonProperty("target_sense")
    public String getTargetSense() {
        return targetSense;
    }

    @JsonProperty("target_sense")
    public void setTargetSense(String targetSense) {
        this.targetSense = targetSense;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("score")
    public Double getScore() {
        return score;
    }

    @JsonProperty("score")
    public void setScore(Double score) {
        this.score = score;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}