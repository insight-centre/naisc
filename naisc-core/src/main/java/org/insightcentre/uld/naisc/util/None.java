package org.insightcentre.uld.naisc.util;

import java.util.AbstractCollection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An option which has no value
 * 
 * @author John McCrae
 */
public final class None<A> extends AbstractCollection<A> implements Option<A> {

    public None() {
    }

    @Override
    public A get() {
        throw new NoSuchElementException();
    }

    @Override
    public A getOrExcept(RuntimeException exception) {
        throw exception;
    }
    
    

    @Override
    public A getOrElse(A a) {
        return a;
    }

    @Override
    public boolean has() {
        return false;
    }

    @Override
    public List<A> toList() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Option<A> or(Option<A> a) {
        return a;
    }

    @Override
    public boolean equals(Object obj) {
        // All Nones are equal
        return obj != null && obj instanceof None;
    }

    @Override
    public int hashCode() {
        return None.class.hashCode();
    }

    @Override
    public Iterator<A> iterator() {
        return new Iterator<A>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public A next() {
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public int size() {
        return 0;
    }
    
    

    
}
