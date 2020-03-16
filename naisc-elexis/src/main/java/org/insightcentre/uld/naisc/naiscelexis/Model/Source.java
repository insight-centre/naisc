package org.insightcentre.uld.naisc.naiscelexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;

public class Source {

    @JsonProperty("endpoint")
    String endpoint;

    @JsonProperty("id")
    @NotEmpty
    String id;

    @JsonProperty("entries")
    ArrayList< String > entries;

    @JsonProperty("apiKey")
    String apiKey;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<String> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<String> entries) {
        this.entries = entries;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
