package org.insightcentre.uld.naisc.main;

/**
 * An error occurred during the configuration of Naisc
 * @author John McCrae
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }

}
