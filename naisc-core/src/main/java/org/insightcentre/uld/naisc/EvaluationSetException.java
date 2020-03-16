package org.insightcentre.uld.naisc;

/**
 * An exception related to a particular dataset
 * @author John McCrae
 */
public class EvaluationSetException extends RuntimeException {

    public EvaluationSetException() {
    }

    public EvaluationSetException(String message) {
        super(message);
    }

    public EvaluationSetException(String message, Throwable cause) {
        super(message, cause);
    }

    public EvaluationSetException(Throwable cause) {
        super(cause);
    }

    public EvaluationSetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
