package org.insightcentre.uld.naisc.elexis.Model;

import org.springframework.http.HttpStatus;

/**
 * Stores the response for submit Linking API
 * @author Suruchi Gupta
 */
public class SubmitResponse {
    private String requestId;

    /**
     * Get unique requestId
     * @return requestId
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Set requestId
     * @param requestId
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
