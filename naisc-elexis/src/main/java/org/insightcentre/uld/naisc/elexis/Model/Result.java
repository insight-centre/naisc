package org.insightcentre.uld.naisc.elexis.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

/**
 * Stores the Result for the linking request
 *
 * @author Suruchi Gupta
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "source_entry",
        "target_entry",
        "linking"
})
@Data
public class Result {

    @JsonProperty("source_entry")
    private String sourceEntry;
    @JsonProperty("target_entry")
    private String targetEntry;
    @JsonProperty("linking")
    private List<Linking> linking = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

}