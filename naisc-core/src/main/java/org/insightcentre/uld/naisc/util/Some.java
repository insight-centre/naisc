package org.insightcentre.uld.naisc.util;

import java.util.AbstractCollection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An option which has a value
 * @author John McCrae
 */
public final class Some<A> extends AbstractCollection<A> implements Option<A> {
    private final A a;

    public Some(A a) {
        this.a = a;
    }

    @Override
    public A get() {
        return a;
    }

    @Override
    public A getOrElse(A a2) {
        return a;
    }

    @Override
    public A getOrExcept(RuntimeException exception) {
        return a;
    }

    @Override
    public Option<A> or(Option<A> a) {
        return this;
    }
    
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.a);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Some<?> other = (Some<?>) obj;
        if (!Objects.equals(this.a, other.a)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean has() {
        return true;
    }

    @Override
    public List<A> toList() {
        return Collections.singletonList(a);
    }

    @Override
    public Iterator<A> iterator() {
        return new Iterator<A>() {
            boolean done = false;
            @Override
            public boolean hasNext() {
                return !done;
            }

            @Override
            public A next() {
                if(done)
                    throw new NoSuchElementException();
                else {
                    done = true;
                    return a;
                }
            }
        };
    }

    @Override
    public int size() {
        return 1;
    }
}
