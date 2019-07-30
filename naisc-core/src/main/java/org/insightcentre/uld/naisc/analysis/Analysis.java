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
    public final double leftLCC, rightLCC;
    public final int leftSize, rightSize;

    public Analysis(List<LabelResult> leftLabels, List<LabelResult> rightLabels, List<MatchResult> matching, Set<String> leftClasses, Set<String> rightClasses, double leftLCC, double rightLCC, int leftSize, int rightSize) {
        this.leftLabels = leftLabels;
        this.rightLabels = rightLabels;
        this.matching = matching;
        this.leftClasses = leftClasses;
        this.rightClasses = rightClasses;
        this.leftLCC = leftLCC;
        this.rightLCC = rightLCC;
        this.leftSize = leftSize;
        this.rightSize = rightSize;
    }
    
    public boolean isWellConnected() {
        return this.leftLCC > 0.01 && this.leftLCC >= 3.0 / leftSize && this.rightLCC > 0.01 && this.rightLCC >= 3.0 / rightSize;
    }

}
