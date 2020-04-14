package org.insightcentre.uld.naisc.rest;

public class DatasetNotFoundException extends Exception {
    public DatasetNotFoundException() {
    }

    public DatasetNotFoundException(String s) {
        super(s);
    }

    public DatasetNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DatasetNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
