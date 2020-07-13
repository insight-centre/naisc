package org.insightcentre.uld.naisc.elexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Stores the Configuration details for the linking request
 *
 * @author Suruchi Gupta
 */
@Data
public class Configuration {

    @JsonProperty("some")
    org.insightcentre.uld.naisc.main.Configuration some;
}
