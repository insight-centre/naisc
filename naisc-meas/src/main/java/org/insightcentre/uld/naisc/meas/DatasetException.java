package org.insightcentre.uld.naisc.meas;

/**
 * An exception related to a particular dataset
 * @author John McCrae
 */
public class DatasetException extends RuntimeException {

    public DatasetException() {
    }

    public DatasetException(String message) {
        super(message);
    }

    public DatasetException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatasetException(Throwable cause) {
        super(cause);
    }

    public DatasetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
