package org.insightcentre.uld.naisc;

/**
 * Tbe result of scoring
 * @author John McCrae
 */
public interface ScoreResult {
    /**
     * Get the result of scoring
     * @return A score value
     */
    double value();
    
    /**
     * Wrap a single double value into a result
     * @param d The score
     * @return Return a boxed score that returns the given value
     */
    public static ScoreResult fromDouble(final double d) {
        return new ScoreResult() {
            @Override
            public double value() {
                return d;
            }

            @Override
            public String toString() {
                return "" + d;
            }
        };
    }
}
