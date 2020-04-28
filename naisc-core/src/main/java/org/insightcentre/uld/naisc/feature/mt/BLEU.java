package org.insightcentre.uld.naisc.feature.mt;

import java.util.List;
import static java.lang.Math.exp;
import static java.lang.Math.pow;

public class BLEU {
    public static double bleuScore(String[] hyp, String[] ref, int N) {
        int[] ngramPrecision = new int[N];
        for(int i = 0; i < hyp.length; i++) {
            boolean[] match = new boolean[N];
            for(int j = 0; j < ref.length; j++) {
                for(int k = 0; k  + j < ref.length && k + i < hyp.length && k < N; k++) {
                    if(hyp[i+k].equals(ref[j + k])) {
                        match[k] = true;
                    } else {
                        break;
                    }
                }
            }
            for(int n = 0; n < N; n++) {
                if(match[n]) ngramPrecision[n]++;
            }
        }
        double score = 1.0;
        for(int i = 0; i < N && i < hyp.length; i++) {
            score *= ngramPrecision[i];
            score /= hyp.length - i;
        }
        score = pow(score, 1.0 / N);
        if(hyp.length < ref.length) {
            score *= exp(1.0 - (double)ref.length / hyp.length);
        }
        return score;
    }
}
