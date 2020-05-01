package org.insightcentre.uld.naisc.feature.mt;

import org.junit.Test;

import static org.junit.Assert.*;

public class chrFTest {

    @Test
    public void testChrF() {
        String hyp = "the quick brown fox jumped over the lazy dog";
        String ref = "the fast brown fox jumped over the sleepy dog";
        double score = chrF.chrF(hyp, ref, 6, 3);
        assertEquals(0.551378, score, 0.0001);
    }
}