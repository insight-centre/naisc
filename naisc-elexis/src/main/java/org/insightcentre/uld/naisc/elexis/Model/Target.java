package org.insightcentre.uld.naisc.elexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.jvnet.hk2.annotations.Optional;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;

/**
 * Stores the Target details for the linking request
 *
 * @author Suruchi Gupta
 */
@Data
public class Target {

    @JsonProperty("endpoint")
    String endpoint;

    @JsonProperty("id")
    @NotEmpty
    String id;

    @JsonProperty("entries")
    @Optional
    ArrayList<String> entries;
}
