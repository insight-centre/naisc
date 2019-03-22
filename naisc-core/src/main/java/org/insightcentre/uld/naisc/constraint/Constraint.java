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
     * the delta in score should be <code>Math.log(p + 1.0 + EPS)</code>
     */
    public static final double EPS = 1e-10;
    /** The score of this alignment */
    public final double score;
    /** The alignments that this score is calculated from 
     * @param alignments The list to add all alignments to
     * @return The list passed
     */
    public abstract List<Alignment> alignments(List<Alignment> alignments);
    
    /**
     * Create an score
     * @param score The score
     */
    protected Constraint(double score) {
        this.score = score;
    }

    
    /**
     * Calculate the change in score that would happen if the alignment were added
     * to the current set of alignments
     * @param alignment The alignment to add
     * @return The score delta, i.e., <code>this.add(alignment).score - this.score</code>
     */
    public double delta(Alignment alignment) {
        return Math.log(alignment.score + 1.0 + EPS);
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
     * @return A new constraint instance
     */
    public abstract Constraint add(Alignment alignment);
    
    /**
     * Is this a complete solution.
     * @return true if the alignment is satisfactorily complete;
     */
    public boolean complete() { 
        return true; 
    }
}
