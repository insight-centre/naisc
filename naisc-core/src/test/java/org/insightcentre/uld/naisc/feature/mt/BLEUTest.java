package org.insightcentre.uld.naisc.feature.mt;

import junit.framework.TestCase;
import org.junit.Test;

public class BLEUTest extends TestCase {

    @Test
    public void testBLEUscore()  {
        String[] hyp = new String[] {"the", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dog" };
        String[] ref = new String[] { "the", "fast", "brown", "fox", "jumped", "over", "the", "sleepy", "dog" };
        double score = BLEU.bleuScore(hyp, ref, 4);
        assertEquals(0.48549177, score, 0.0001);
    }
}