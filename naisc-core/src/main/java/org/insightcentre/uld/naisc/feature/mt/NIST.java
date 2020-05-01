package org.insightcentre.uld.naisc.feature.mt;

public class NIST {

    public static double nistScore(String[] hyp, String[] ref, int N) {
        double ratio = ref.length / hyp.length;
        if (ratio >= 1.0)
            return 1.0;
        if (ratio <= 0.0)
            return 0.0;
        double ratio_x = 1.5, score_x = .5;
        double beta = -Math.log(score_x) / Math.log(ratio_x) / Math.log(ratio_x);
        double brevityPenalty =  Math.exp(-beta * Math.log(ratio) * Math.log(ratio));

        double ngramScore = 0;
        double[] precisions = BLEU.ngramPrecision(hyp, ref, N);
        for (int i = 0; i < N; i++) {
            double p = precisions[i];
            ngramScore += !Double.isNaN(p) ? p : 0;
        }
        return ngramScore * brevityPenalty;
    }
}
