package org.insightcentre.uld.naisc.feature.mt;

import org.junit.Test;

import static org.junit.Assert.*;

public class TERTest {

    @Test
    public void testTERscore()  {
        String hyp = "the quick brown fox jumped over the lazy dog";
        String ref = "the fast brown fox jumped over the sleepy dog";
        double score = TER.terScore(hyp, ref);
        assertEquals(0.77777777, score, 0.0001);
    }
}