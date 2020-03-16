package org.insightcentre.uld.naisc.util;

import java.util.function.Supplier;

/**
 * A lazy loaded variable 
 * @author John McCrae
 */
public abstract class Lazy<X> {

    private X x;

    public X get() {
        if (x == null) {
            x = init();
        }
        return x;
    }

    protected abstract X init();
    
    public static <Y> Lazy<Y> fromClosure(Supplier<Y> f) {
        return new Lazy<Y>() {
            @Override
            protected Y init() { return f.get(); }
        };
    }
}
