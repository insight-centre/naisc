package org.insightcentre.uld.naisc.elexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jvnet.hk2.annotations.Optional;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class Target {

    @JsonProperty("endpoint")
    static String endpoint;

    @JsonProperty("id")
    @NotEmpty
    static String id;

    @JsonProperty("entries")
    @Optional
    static String[] entries;

    public static String getEndpoint() {
        return endpoint;
    }

    public static void setEndpoint(String endpoint) {
        Target.endpoint = endpoint;
    }

    public static String getId() {
        return id;
    }

    public static void setId(String id) {
        Target.id = id;
    }

    public static String[] getEntries() {
        return entries;
    }

    public static void setEntries(String[] entries) {
        Target.entries = entries;
    }
}
