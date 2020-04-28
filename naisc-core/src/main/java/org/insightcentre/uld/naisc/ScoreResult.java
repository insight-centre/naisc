package org.insightcentre.uld.naisc;

/**
 * Tbe result of scoring
 * @author John McCrae
 */
public interface ScoreResult {
    /**
     * Get the result of scoring
     * @return A probability value
     */
    double value();
    
    /**
     * Wrap a single double value into a result
     * @param d The probability
     * @return Return a boxed probability that returns the given value
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
