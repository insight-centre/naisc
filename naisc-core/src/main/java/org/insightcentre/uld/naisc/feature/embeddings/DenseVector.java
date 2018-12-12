package org.insightcentre.uld.naisc.feature.embeddings;

import org.insightcentre.uld.naisc.util.LinearAlgebraException;
import org.insightcentre.uld.naisc.util.Vector;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.ints.AbstractInt2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Collection;

/**
 * A dense vector (e.g., a double array)
 * 
 * @author John McCrae
 */
public class DenseVector extends DoubleArrayList implements Vector {
    public DenseVector(double[] a) {
        super(a);
    }

    public DenseVector(int capacity) {
        super(capacity);
        for(int i = 0; i < capacity; i++) {
            this.push(0.0);
        }
    }

    public DenseVector(Collection<? extends Double> c) {
        super(c);
    }

    public DenseVector(DoubleCollection c) {
        super(c);
    }
    
    @Override
    public ObjectSet<Int2DoubleMap.Entry> sparseEntrySet() {
        return new AbstractObjectSet<Int2DoubleMap.Entry>() {

            @Override
            public ObjectIterator<Int2DoubleMap.Entry> iterator() {
                return new ObjectIterator<Int2DoubleMap.Entry>() {
                    int n = 0;

                    @Override
                    public int skip(int i) {
                        int nold = n;
                        n = Math.max(0, Math.min(size(), n + i));
                        return n - nold;
                    }

                    @Override
                    public boolean hasNext() {
                        return n < size();
                    }

                    @Override
                    public Int2DoubleMap.Entry next() {
                        return new AbstractInt2DoubleMap.BasicEntry(n, getDouble(n++));
                    }

                    @Override
                    public void remove() {
                    }
                };
            }

            @Override
            public int size() {
                return DenseVector.this.size();
            }
        };
    }

    @Override
    public double sum() {
        double sum = 0;
        for(int i = 0; i < a.length; i++) {
            sum += a[i];
        }
        return sum;
    }

    @Override
    public double norm() {
        double sum = 0;
        for(int i = 0; i < a.length; i++) {
            sum += a[i] * a[i];
        }
        return Math.sqrt(sum);
    }

    @Override
    public Vector add(Vector v) {
        if(v.len() != a.length) throw new LinearAlgebraException(String.format("Vector lengths do not match %d <-> %d", v.len(), a.length));
        for(int i = 0; i <  a.length; i++) {
            a[i] += v.getDouble(i);
        }
        return this;
    }
    
    @Override
    public Vector add(Vector v, double d) {
        if(v.len() != a.length) throw new LinearAlgebraException(String.format("Vector lengths do not match %d <-> %d", v.len(), a.length));
        for(int i = 0; i <  a.length; i++) {
            a[i] += v.getDouble(i) * d;
        }
        return this;
    }


    @Override
    public Vector scale(double d) {
        for(int i = 0; i < a.length; i++) {
            a[i] *= d;
        }
        return this;
    }

    @Override
    public Vector tanh() {
        for(int i = 0; i < a.length; i++) {
            a[i] = Math.tanh(a[i]);
        }
        return this;
    }


    @Override
    public Vector sigmoid() {
        for(int i = 0; i < a.length; i++) {
            a[i] = 1.0 / (1.0 + Math.exp(-a[i]));
        }
        return this;
    }


    @Override
    public Vector abs() {
        for(int i = 0; i < a.length; i++) {
            a[i] = Math.abs(a[i]);
        }
        return this;
    }

    @Override
    public Vector pairwise(Vector v) {
        if(v.len() != a.length) throw new LinearAlgebraException(String.format("Vector lengths do not match %d <-> %d", v.len(), a.length));
        DenseVector v2 = new DenseVector(a.length);
        for(int i = 0; i < a.length; i++) {
            v2.set(i, a[i] * v.getDouble(i));
        }
        return v2;
    }

    @Override
    public int len() {
        return a.length;
    }

    
}
