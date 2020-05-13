    package org.insightcentre.uld.naisc.elexis.Model;

        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import com.fasterxml.jackson.annotation.JsonAnyGetter;
        import com.fasterxml.jackson.annotation.JsonAnySetter;
        import com.fasterxml.jackson.annotation.JsonIgnore;
        import com.fasterxml.jackson.annotation.JsonInclude;
        import com.fasterxml.jackson.annotation.JsonProperty;
        import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "source_entry",
        "target_entry",
        "linking"
})
public class Result {

    @JsonProperty("source_entry")
    private String sourceEntry;
    @JsonProperty("target_entry")
    private String targetEntry;
    @JsonProperty("linking")
    private List<Linking> linking = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("source_entry")
    public String getSourceEntry() {
        return sourceEntry;
    }

    @JsonProperty("source_entry")
    public void setSourceEntry(String sourceEntry) {
        this.sourceEntry = sourceEntry;
    }

    @JsonProperty("target_entry")
    public String getTargetEntry() {
        return targetEntry;
    }

    @JsonProperty("target_entry")
    public void setTargetEntry(String targetEntry) {
        this.targetEntry = targetEntry;
    }

    @JsonProperty("linking")
    public List<Linking> getLinking() {
        return linking;
    }

    @JsonProperty("linking")
    public void setLinking(List<Linking> linking) {
        this.linking = linking;
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