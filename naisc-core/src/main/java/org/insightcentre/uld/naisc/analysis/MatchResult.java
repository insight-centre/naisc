package org.insightcentre.uld.naisc.analysis;

/**
 * The result of analysing a matching
 * @author John McCrae
 */
public class MatchResult {

    public final String leftUri;
    public final String rightUri;
    public final int leftTotal;
    public final int rightTotal;
    public final int coverage;

    public MatchResult(String leftUri, String rightUri, int leftTotal, int rightTotal, int coverage) {
        this.leftUri = leftUri;
        this.rightUri = rightUri;
        this.leftTotal = leftTotal;
        this.rightTotal = rightTotal;
        this.coverage = coverage;
    }

}
