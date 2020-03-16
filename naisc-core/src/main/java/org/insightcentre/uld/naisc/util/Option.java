package org.insightcentre.uld.naisc.util;

import java.util.Collection;
import java.util.List;

/**
 * A value that may or may not be there
 * @author John McCrae
 */
public interface Option<A> extends Collection<A> {
    /** 
     * Get the value contained
     * @return The value of this option if Some
     * @throws java.util.NoSuchElementException If this is None
     */
    A get();
    /**
     * Get the value contained or a default value 
     * @param a The value to use if this is None
     * @return The value of this if Some, or the given value
     */
    A getOrElse(A a);
    /**
     * Get the value of raise an exception
     * @param exception The exception to raise
     * @returns A if this value is some
     */
    A getOrExcept(RuntimeException exception);
    /**
     * Is this Some
     * @return True if Some, false if None
     */
    boolean has();
    /**
     * Convert to a list of values
     * @return A zero or one element list
     */
    List<A> toList();
    /**
     * Return either this value or the next value, prefering this value
     * @param a The second option
     * @return This if Some, a if None
     */
    Option<A> or(Option<A> a);
}
