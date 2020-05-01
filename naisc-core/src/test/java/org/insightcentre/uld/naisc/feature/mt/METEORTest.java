package org.insightcentre.uld.naisc.feature.mt;

import org.junit.Test;

import static org.junit.Assert.*;

public class METEORTest {

    @Test

    public void testMeteorScore()  {
        String hyp = "the quick brown fox jumped over the lazy dog";
        String ref = "the fast brown fox jumped over the sleepy dog";
        double score = METEOR.meteorScore(hyp, ref);
        assertEquals(0.352519, score, 0.0001);
    }
}