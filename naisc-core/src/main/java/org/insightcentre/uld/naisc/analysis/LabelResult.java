package org.insightcentre.uld.naisc.analysis;

import org.apache.jena.vocabulary.RDFS;

/**
 * The result of analysing a single label property
 * 
 * @author John McCrae
 */
public class LabelResult {

    public final String uri;
    public final int total;
    public final double coverage;
    public final double naturalLangLike;
    public final double uniqueness;
    public final double diversity;
    public final boolean isDataProperty;

    public LabelResult(String uri, int total, double coverage, double naturalLangLike, double uniqueness, double diversity, boolean isDataProperty) {
        this.uri = uri;
        this.total = total;
        this.coverage = coverage;
        this.naturalLangLike = naturalLangLike;
        this.uniqueness = uniqueness;
        this.diversity = diversity;
        this.isDataProperty = isDataProperty;
    }
    
    public boolean isLabelLike() {
        return this.coverage > 0.5 && this.uniqueness > 0.5 && this.isDataProperty && this.naturalLangLike > 0.5;
    }
    
    public boolean isLabelLens() {
        return (this.coverage > 0.5 && this.uniqueness > 0.1 && (this.uri.equals("") || this.isDataProperty) && this.naturalLangLike > 0.5)
            || this.uri.equals(RDFS.label.getURI());
    }

    @Override
    public String toString() {
        return "LabelResult{" +
                "uri='" + uri + '\'' +
                ", total=" + total +
                ", coverage=" + coverage +
                ", naturalLangLike=" + naturalLangLike +
                ", uniqueness=" + uniqueness +
                ", diversity=" + diversity +
                ", isDataProperty=" + isDataProperty +
                '}';
    }
}
