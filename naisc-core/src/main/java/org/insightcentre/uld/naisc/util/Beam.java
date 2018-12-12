package org.insightcentre.uld.naisc.util;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import java.util.Comparator;
import java.util.Iterator;

/**
 * A 'beam' that is a sorted set of fixed size where adding an element remove the
 * lowest scoring element.
 * 
 * @author John McCrae
 */
public class Beam<A> implements Iterable<A> {
    private final Object2DoubleMap<A> scores = new Object2DoubleOpenHashMap<>();
    private final ObjectSortedSet<A> queue = new ObjectRBTreeSet<A>(new ScoreComparator<A>());
    private final int size;

    public Beam(int size) {
        this.size = size;
    }

    @Override
    public Iterator<A> iterator() {
        return queue.iterator();
    }
    
    public int size() {
        return queue.size();
    }
    
    
    private class ScoreComparator<A> implements Comparator<A> {

        @Override
        public int compare(A o1, A o2) {
            double score1 = scores.getDouble(o1);
            double score2 = scores.getDouble(o2);
            if(score1 > score2) {
                return -1;
            } else if(score1 < score2) {
                return +1;
            } else if(o1.equals(o2)) {
                return 0;
            } else {
                int h1 = o1.hashCode();
                int h2 = o2.hashCode();
                if(h1 < h2) {
                    return -1;
                } else if (h1 > h2) {
                    return +1;
                } else {
                    System.err.println("Comparable error");
                    return 0;
                }
            }
        }
    }
    
    public void insert(A a, double score) {
        if(queue.size() <= size || score > scores.getDouble(queue.last())) {
            scores.put(a, score);
            queue.add(a);
            if(queue.size() > size) {
                queue.remove(queue.last());
            }
        }
    }
    
    public A peek() {
        return queue.first();
    }
    
    public A poll() {
        A a = queue.first();
        queue.remove(a);
        scores.remove(a);
        return a;
    }
    
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
        scores.clear();
    }
}
