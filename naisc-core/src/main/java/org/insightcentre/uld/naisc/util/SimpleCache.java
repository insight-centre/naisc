package org.insightcentre.uld.naisc.util;

import java.util.HashMap;
import java.util.Iterator;

/**
 * A LRU cache for any objects. Please note that this is NOT thread-safe!
 * 
 * @author John McCrae
 */
public class SimpleCache<E,F> {
    private static final class CacheEntry<E> {
        public final E i;
        public int age;

        public CacheEntry(E i, int age) {
            this.i = i;
            this.age = age;
        }
    }
    
    /**
     * SAM to compute a cached entry
     * @param <E> The key type
     * @param <F> The value type
     */
    public static interface Get<E,F> {
        public F get(E e);
    }
    
    private final HashMap<E, CacheEntry<F>> data = new HashMap<>();
    private int age = 0;
    private final int capacity;
    private final int threshold;
    private int tooOld = 0;

    /**
     * Create a cache
     * @param capacity The maximum capacity
     */
    public SimpleCache(int capacity) {
        this.capacity = capacity;
        this.threshold = capacity * 10 / 9;
    }

    /**
     * Get the object for the key or use get to compute it
     * @param e The key
     * @param get The value computer
     * @return The result of get.get(e) possibly from the cache
     */
    public F get(E e, Get<E, F> get) {
        if(data.containsKey(e)) {
            CacheEntry<F> ce = data.get(e);
            ce.age = age++;
            return ce.i;
        } else {
            CacheEntry<F> ce = new CacheEntry(get.get(e), age++);
            data.put(e, ce);
            while(data.size() > capacity) {
                tooOld += Math.max(1,capacity / 10);
                Iterator<CacheEntry<F>> iter = data.values().iterator();
                while(iter.hasNext()) {
                    if(iter.next().age < tooOld) {
                        iter.remove();
                    }
                }
            }
            return ce.i;
        }
    }
}
