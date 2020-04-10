package org.insightcentre.uld.naisc.constraint;

import java.util.List;
import org.insightcentre.uld.naisc.Alignment;

/**
 * A constraint, which both scores the current partial solution, and also decides
 * how a solution may be extended. This should generally be treated as immutable
 * @author John McCrae
 */
public abstract class Constraint {
    /** An epsilon value that can be used in calculating scores. Generally
     * the delta in probability should be <code>Math.log(p + 1.0 + EPS)</code>
     */
    public static final double EPS = 1e-10;
    /** The probability of this alignment */
    public double score;
    
    /** 
     * The alignments that this probability is calculated from
     * @return The list passed
     */
    public abstract List<Alignment> alignments();
    
    /**
     * Create an probability
     * @param score The probability
     */
    protected Constraint(double score) {
        this.score = score;
    }

    
    /**
     * Calculate the change in probability that would happen if the alignment were added
     * to the current set of alignments
     * @param alignment The alignment to add
     * @return The probability delta, i.e., <code>this.add(alignment).probability - this.probability</code>
     */
    public double delta(Alignment alignment) {
        return Math.log(alignment.probability + 1.0 + EPS);
    }

    /**
     * Can this alignment be added to the current set of alignments. This implements
     * hard constraints such as bipartite matching.
     * @param alignment The alignment
     * @return true if the alignment can be added
     */    
    public abstract boolean canAdd(Alignment alignment);

    /**
     * Create a new alignment of the same type with the alignment added. In 
     * implementing this it is very important that all data stored by this constraint
     * including the alignments list are cloned, otherwise the algorithm can proceed
     * in a very strange manner
     * @param alignment The additional alignment
     */
    public abstract void add(Alignment alignment);
    
    /**
     * Create a clone of this constraint (for example to model multiple paths in the solution space
     * @return A copy of this constraint
     */
    public abstract Constraint copy();
    
    /**
     * Returns whether adding the alignment would complete the constraint
     * @param alignment The alignment to add
     * @return true if after adding the aligment complete() would return true
     */
    public boolean canComplete(Alignment alignment) {
        return true;
    }
    
    /**
     * Is this a complete solution.
     * @return true if the alignment is satisfactorily complete;
     */
    public boolean complete() { 
        return true; 
    }
}
