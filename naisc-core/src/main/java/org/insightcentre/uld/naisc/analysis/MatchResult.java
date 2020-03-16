package org.insightcentre.uld.naisc.analysis;

import java.util.Objects;

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

    public boolean coversData() {
        return this.coverage > 100 || this.coverage > 0.1 * Math.max(this.leftTotal, this.rightTotal);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.leftUri);
        hash = 37 * hash + Objects.hashCode(this.rightUri);
        hash = 37 * hash + this.leftTotal;
        hash = 37 * hash + this.rightTotal;
        hash = 37 * hash + this.coverage;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MatchResult other = (MatchResult) obj;
        if (this.leftTotal != other.leftTotal) {
            return false;
        }
        if (this.rightTotal != other.rightTotal) {
            return false;
        }
        if (this.coverage != other.coverage) {
            return false;
        }
        if (!Objects.equals(this.leftUri, other.leftUri)) {
            return false;
        }
        if (!Objects.equals(this.rightUri, other.rightUri)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MatchResult{" + "leftUri=" + leftUri + ", rightUri=" + rightUri + ", leftTotal=" + leftTotal + ", rightTotal=" + rightTotal + ", coverage=" + coverage + '}';
    }
    
    
}
