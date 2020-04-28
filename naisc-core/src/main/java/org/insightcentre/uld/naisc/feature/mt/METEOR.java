package org.insightcentre.uld.naisc.feature.mt;

import edu.cmu.meteor.scorer.MeteorScorer;
import edu.cmu.meteor.scorer.MeteorStats;

public class METEOR {

    private static MeteorScorer scorer = new MeteorScorer();
    public static double meteorScore(String s1, String s2) {
        MeteorStats stats = scorer.getMeteorStats(s1, s2);
        scorer.computeMetrics(stats);
        return stats.score;
    }
}
