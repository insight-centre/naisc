package org.insightcentre.uld.naisc.constraint;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.insightcentre.uld.naisc.Alignment;

/**
 * A constraint of strict bijectivity, that is no element on either side may be
 * linked to more than one element on the other side. Note this constraint can
 * be more efficiently solved with the
 * {@link org.insightcentre.uld.naisc.matcher.UniqueAssignment} solver.
 *
 * @author John McCrae
 */
public class Bijective implements ConstraintFactory {

    /**
     * Configuration of bijective constraint. There are currently no parameters
     */
    public static class Configuration {
    }

    @Override
    public Constraint make(Map<String, Object> params) {
        return new BijectiveImpl(new ArrayList<>(), 0);
    }

    private static class BijectiveImpl extends Constraint {
        // Data is stored either as a hashset (to allow efficient querying)
        // or by storing the left and right and the link to the parents
        // and back-tracking (memory efficient but slow querying)
        // The soft references ensure that Java won't crash but will slow
        // down for big problems
        SoftReference<HashSet<String>> l2r;
        SoftReference<HashSet<String>> r2l;
        final String l, r;
        final BijectiveImpl parent;

        public BijectiveImpl(List<Alignment> alignments, double score) {
            super(alignments, score);
            HashSet<String> l2r = new HashSet<>();
            HashSet<String> r2l = new HashSet<>();
            if (!alignments.isEmpty()) {
                for (Alignment alignment : alignments) {
                    l2r.add(alignment.entity1);
                    r2l.add(alignment.entity2);
                }
            }
            this.l2r = new SoftReference<>(l2r);
            this.r2l = new SoftReference<>(r2l);
            this.l = null;
            this.r = null;
            this.parent = null;
        }

        public BijectiveImpl(HashSet<String> l2r, HashSet<String> r2l,
                List<Alignment> alignments, double score, String l, String r,
                BijectiveImpl parent) {
            super(alignments, score);
            this.l2r = new SoftReference<>(l2r);
            this.r2l = new SoftReference<>(r2l);
            this.l = l;
            this.r = r;
            this.parent = parent;
        }

        @Override
        public Constraint add(Alignment alignment) {
            HashSet<String> newl2r = (HashSet<String>) this.l2r().clone();
            HashSet<String> newr2l = (HashSet<String>) this.r2l().clone();
            newl2r.add(alignment.entity1);
            newr2l.add(alignment.entity2);
            List<Alignment> newaligments = new ArrayList<>(this.alignments);
            newaligments.add(alignment);
            double newscore = this.score + delta(alignment);
            return new BijectiveImpl(newl2r, newr2l, newaligments, newscore,
                    alignment.entity1, alignment.entity2, this);
        }

        @Override
        public boolean canAdd(Alignment alignment) {
            return !l2r().contains(alignment.entity1) && !r2l().contains(alignment.entity2);
        }

        private HashSet<String> l2r() {
            return l2r(true);
        }
        
        private HashSet<String> l2r(boolean head) {
            HashSet<String> s = this.l2r.get();
            if (s != null) {
                return s;
            } else if (l != null) {
                s = (HashSet<String>) parent.l2r(false).clone();
                s.add(l);
                if(head)
                    this.l2r = new SoftReference(s);
                return s;
            } else {
                return new HashSet<>();
            }
        }

        private HashSet<String> r2l() {
            return r2l(true);
        }
        
        private HashSet<String> r2l(boolean head) {
            HashSet<String> s = this.r2l.get();
            if (s != null) {
                return s;
            } else if (r != null) {
                s = (HashSet<String>) parent.r2l(false).clone();
                s.add(r);
                if(head)
                    this.r2l = new SoftReference<>(s);
                return s;
            } else {
                return new HashSet<>();
            }
        }

    }

}
