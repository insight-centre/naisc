package org.insightcentre.uld.naisc.analysis;

import java.util.List;
import java.util.Set;

/**
 * The result of analysing two datasets.
 * @author John McCrae
 */
public class Analysis {

    public final List<LabelResult> leftLabels;
    public final List<LabelResult> rightLabels;
    public final List<MatchResult> matching;
    public final Set<String> leftClasses, rightClasses;

    public Analysis(List<LabelResult> leftLabels, List<LabelResult> rightLabels, List<MatchResult> matching, Set<String> leftClasses, Set<String> rightClasses) {
        this.leftLabels = leftLabels;
        this.rightLabels = rightLabels;
        this.matching = matching;
        this.leftClasses = leftClasses;
        this.rightClasses = rightClasses;
    }


}
