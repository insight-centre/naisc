package org.insightcentre.uld.naisc.naiscelexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jvnet.hk2.annotations.Optional;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;

public class Target {

    @JsonProperty("endpoint")
    String endpoint;

    @JsonProperty("id")
    @NotEmpty
    String id;

    @JsonProperty("entries")
    @Optional
    ArrayList<Object> entries = new ArrayList<Object>();

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

    public ArrayList<Object> getEntries() {
        return entries;
    }

    public void setEntries(String[] entries) {
        this.entries.add(entries);
    }
}
