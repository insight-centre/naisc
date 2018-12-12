package org.insightcentre.uld.naisc.util;

/**
 * An invalid linear algebra operation was requested
 * 
 * @author John McCrae
 */
public class LinearAlgebraException extends RuntimeException {

    public LinearAlgebraException() {
    }

    public LinearAlgebraException(String message) {
        super(message);
    }

    public LinearAlgebraException(String message, Throwable cause) {
        super(message, cause);
    }

    public LinearAlgebraException(Throwable cause) {
        super(cause);
    }
    
}
