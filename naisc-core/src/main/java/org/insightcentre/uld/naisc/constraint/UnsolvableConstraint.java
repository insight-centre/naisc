package org.insightcentre.uld.naisc.constraint;

/**
 * A constrain could not be solved with the current solver.
 * 
 * @author John McCrae
 */
public class UnsolvableConstraint extends RuntimeException {

    public UnsolvableConstraint() {
    }

    public UnsolvableConstraint(String message) {
        super(message);
    }

    public UnsolvableConstraint(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsolvableConstraint(Throwable cause) {
        super(cause);
    }

}
