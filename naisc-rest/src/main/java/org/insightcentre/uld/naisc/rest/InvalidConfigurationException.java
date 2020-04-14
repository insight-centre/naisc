package org.insightcentre.uld.naisc.rest;

public class InvalidConfigurationException extends Exception {

    public InvalidConfigurationException() {
    }

    public InvalidConfigurationException(String s) {
        super(s);
    }

    public InvalidConfigurationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InvalidConfigurationException(Throwable throwable) {
        super(throwable);
    }

    public InvalidConfigurationException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
