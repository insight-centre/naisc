package org.insightcentre.uld.naisc.util;

/**
 * An error occured with an external command (for example file not found)
 * @author John McCrae
 */
public class ExternalCommandException extends RuntimeException {

    public ExternalCommandException() {
    }

    public ExternalCommandException(String message) {
        super(message);
    }

    public ExternalCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalCommandException(Throwable cause) {
        super(cause);
    }

}
