package org.insightcentre.uld.naisc.elexis.Model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

/**
 * Stores the Linking details for the Result of the linking request
 *
 * @author Suruchi Gupta
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "source_sense",
        "target_sense",
        "type",
        "score"
})
@Data
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
}