package org.insightcentre.uld.naisc.analysis;

import java.util.List;

/**
 * The result of analysing two datasets.
 * @author John McCrae
 */
public class Analysis {

    public final List<LabelResult> leftLabels;
    public final List<LabelResult> rightLabels;
    public final List<MatchResult> matching;

    public Analysis(List<LabelResult> leftLabels, List<LabelResult> rightLabels, List<MatchResult> matching) {
        this.leftLabels = leftLabels;
        this.rightLabels = rightLabels;
        this.matching = matching;
    }

}
