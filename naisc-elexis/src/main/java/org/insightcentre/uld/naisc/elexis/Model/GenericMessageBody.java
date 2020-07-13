package org.insightcentre.uld.naisc.elexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * Stores the MessageBody for the status and result request
 *
 * @author Suruchi Gupta
 */
@Data
public class GenericMessageBody {
    @NotEmpty
    @JsonProperty("requestId")
    String requestId;
}
