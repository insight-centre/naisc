package org.insightcentre.uld.naisc.feature.mt;

import edu.umd.cs.TERalignment;
import edu.umd.cs.TERcalc;
import edu.umd.cs.TERcost;

public class TER {
    private static TERcost costfunc = new TERcost();
    public static double terScore(String hyp, String ref) {
        TERalignment result = TERcalc.TER(hyp, ref, costfunc);
        return 1.0 - result.numEdits / result.numWords;
    }
}
