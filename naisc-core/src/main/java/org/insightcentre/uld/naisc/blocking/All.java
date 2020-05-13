package org.insightcentre.uld.naisc.blocking;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * A blocking strategy that selects all elements in both the left and right
 * model as potential matches. This is for brute force alignment only
 *
 * @author John McCrae
 */
public class All implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> analysis, NaiscListener listener) {
        return new AllImpl();
    }

    private static class AllImpl implements BlockingStrategy {

        @Override
        public Collection<Blocking> block(Dataset left, Dataset right, NaiscListener log) {
            return new AbstractCollection<Blocking>() {
                @Override
                public Iterator<Blocking> iterator() {
                    return new AllIterator(left, right);
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException("Size not known");
                }
            };
        }

        @Override
        public int estimateSize(Dataset left, Dataset right) {
            Iterator<Resource> i1 = left.listSubjects();
            Iterator<Resource> i2 = right.listSubjects();
            int n = 0;
            while(i1.hasNext()) {
                n++;
                i1.next();
            }
            int m = 0;
            while(i2.hasNext()) {
                m++;
                i2.next();
            }
            return n * m;
        }
        
        

    }

    private static class AllIterator implements Iterator<Blocking> {

        private Iterator<Resource> left, right;
        private final Dataset leftModel, rightModel;
        private  Pair<org.apache.jena.rdf.model.Resource, org.apache.jena.rdf.model.Resource> next;

        public AllIterator(Dataset left, Dataset right) {
            this.leftModel = left;
            this.rightModel = right;
            this.left = left.listSubjects();
            this.right = right.listSubjects();
            if(this.left.hasNext() && this.right.hasNext()) {
                this.next = new Pair<>(this.left.next(), this.right.next());
                if(!this.next._1.isURIResource() || !this.next._2.isURIResource()) {
                    advance();
                }
            } else {
                this.next = null;
            }
        }

        private void advance() {
            if(!next._1.isURIResource()) {
                if(left.hasNext()) {
                    this.right = rightModel.listSubjects();
                    next = new Pair(left.next(), null);
                    advance();
                } else {
                    next = null;
                }
            } else if (right.hasNext()) {
                Resource r = right.next();
                while (!r.isURIResource() && right.hasNext()) {
                    r = right.next();
                }
                if (r.isURIResource()) {
                    next = new Pair(next._1, r);
                } else if (left.hasNext()) {
                    this.right = rightModel.listSubjects();
                    Resource l = left.next();
                    while (!l.isURIResource() && left.hasNext()) {
                        l = left.next();
                    }
                    next = new Pair(l, null);
                    advance();
                } else {
                    next = null;
                }
            } else if (left.hasNext()) {
                this.right = rightModel.listSubjects();
                Resource l = left.next();
                while (!l.isURIResource() && left.hasNext()) {
                    l = left.next();
                }
                if(!l.isURIResource()) {
                    next = null;
                } else {
                    next = new Pair(l, null);
                    advance();
                }
            } else {
                next = null;
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Blocking next() {
            Blocking n = new Blocking(next._1, next._2, leftModel.id(), rightModel.id());
            advance();
            return n;
        }

    }

    /**
     * The configuration of the all class. There is no configuration needed.
     */
    public static class Configuration {
        // No configuration yet
    }
}
