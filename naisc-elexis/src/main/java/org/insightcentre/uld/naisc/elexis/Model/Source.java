package org.insightcentre.uld.naisc.elexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;

/**
 * Stores the Source details for the linking request
 *
 * @author Suruchi Gupta
 */
@Data
public class Source {

    @JsonProperty("endpoint")
    String endpoint;

    @JsonProperty("id")
    @NotEmpty
    String id;

    @JsonProperty("entries")
    ArrayList<String> entries = new ArrayList<>();

    @JsonProperty("apiKey")
    String apiKey;
}
