package org.insightcentre.uld.naisc.elexis.Model;

import lombok.Data;

/**
 * Stores the response for /status API
 *
 * @author Suruchi Gupta
 */
@Data
public class StatusResponse {
    String state, message;
}
