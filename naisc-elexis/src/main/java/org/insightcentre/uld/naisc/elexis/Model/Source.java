package org.insightcentre.uld.naisc.elexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;

public class Source {

    @JsonProperty("endpoint")
    static String endpoint;

    @JsonProperty("id")
    @NotEmpty
    static String id;

    @JsonProperty("entries")
    static ArrayList< String > entries;

    @JsonProperty("apiKey")
    static String apiKey;

    public static String getEndpoint() {
        return endpoint;
    }

    public static void setEndpoint(String endpoint) {
        Source.endpoint = endpoint;
    }

    public static String getId() {
        return id;
    }

    public static void setId(String id) {
        Source.id = id;
    }

    public static ArrayList<String> getEntries() {
        return entries;
    }

    public static void setEntries(ArrayList<String> entries) {
        Source.entries = entries;
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static void setApiKey(String apiKey) {
        Source.apiKey = apiKey;
    }
}
