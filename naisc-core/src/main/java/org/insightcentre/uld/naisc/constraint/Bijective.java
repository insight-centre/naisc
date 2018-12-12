package org.insightcentre.uld.naisc.constraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;

/**
 * A constraint of strict bijectivity, that is no element on either side may
 * be linked to more than one element on the other side. Note this constraint can be more 
 * efficiently solved with the {@link org.insightcentre.uld.naisc.matcher.UniqueAssignment}
 * solver.
 * 
 * @author John McCrae
 */
public class Bijective implements ConstraintFactory {
    /**
     * Configuration of bijective constraint. There are currently no parameters
     */
    public static class Configuration {}
    
    @Override
    public Constraint make(Map<String, Object> params) {
        return new BijectiveImpl(new ArrayList<>(), 0);
    }
    
    private static class BijectiveImpl extends Constraint {
        final HashMap<String,String> l2r;
        final HashMap<String,String> r2l;

        public BijectiveImpl(List<Alignment> alignments, double score) {
            super(alignments, score);
            this.l2r = new HashMap<>();
            this.r2l = new HashMap<>();
            if(!alignments.isEmpty()) {
                for(Alignment alignment : alignments) {
                    l2r.put(alignment.entity1, alignment.entity2);
                    r2l.put(alignment.entity2, alignment.entity1);
                }
            }
        }

        public BijectiveImpl(HashMap<String, String> l2r, HashMap<String, String> r2l, List<Alignment> alignments, double score) {
            super(alignments, score);
            this.l2r = l2r;
            this.r2l = r2l;
        }
                
        @Override
        public Constraint add(Alignment alignment) {
            HashMap<String, String> newl2r = (HashMap<String,String>)this.l2r.clone();
            HashMap<String, String> newr2l = (HashMap<String,String>)this.r2l.clone();
            newl2r.put(alignment.entity1, alignment.entity2);
            newr2l.put(alignment.entity2, alignment.entity1);
            List<Alignment> newaligments = new ArrayList<>(this.alignments);
            newaligments.add(alignment);
            double newscore = this.score + delta(alignment);
            return new BijectiveImpl(newl2r, newr2l, newaligments, newscore);
        }

        @Override
        public boolean canAdd(Alignment alignment) {
            return !l2r.containsKey(alignment.entity1) && !r2l.containsKey(alignment.entity2);
        }
        
    }

}
