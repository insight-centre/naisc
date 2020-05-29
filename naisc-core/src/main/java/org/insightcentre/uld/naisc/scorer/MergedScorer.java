package org.insightcentre.uld.naisc.scorer;

import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.ScoreResult;
import org.insightcentre.uld.naisc.Scorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A scorer that delegates scoring to one or more scorers
 * @author John McCrae
 */
public class MergedScorer implements Scorer {
    private final Scorer scorer1, scorer2;

    public MergedScorer(Scorer scorer1, Scorer scorer2) {
        this.scorer1 = scorer1;
        this.scorer2 = scorer2;
    }

    @Override
    public List<ScoreResult> similarity(FeatureSet features, NaiscListener log) throws ModelNotTrainedException{
        List<ScoreResult> results = new ArrayList<>();
        results.addAll(scorer1.similarity(features, log));
        results.addAll(scorer2.similarity(features, log));
        return results;
    }

    @Override
    public void close() throws IOException {
        scorer1.close();
        scorer2.close();
    }
}
