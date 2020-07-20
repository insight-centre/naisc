package org.insightcentre.uld.naisc.feature.mt;

/**
 * Based on
 * CHRF: character n-gram F-score for automatic MT evaluation
 * Maja Popovic
 *
 * This work suggests the values N=6 and beta=3
 */
public class chrF {

    public static double chrF(String hyp, String ref, int N, double beta) {
        double precision = 0.0, recall = 0.0;
        for(int i = 0; i < hyp.length(); i++) {
            boolean[] match = new boolean[N];
            for(int j = 0; j < ref.length(); j++) {
                for(int k = 0; k  + j < ref.length() && k + i < hyp.length() && k < N; k++) {
                    if(hyp.charAt(i+k) == ref.charAt(j + k)) {
                        match[k] = true;
                    } else {
                        break;
                    }
                }
            }
            if(match[N-1]) {
                precision += 1.0 / (hyp.length() - N + 1);
            }
        }
        for(int j = 0; j < ref.length(); j++) {
            boolean[] match = new boolean[N];
            for(int i = 0; i < hyp.length(); i++) {
                for(int k = 0; k  + j < ref.length() && k + i < hyp.length() && k < N; k++) {
                    if(hyp.charAt(i+k) == ref.charAt(j + k)) {
                        match[k] = true;
                    } else {
                        break;
                    }
                }
            }
            if(match[N-1]) {
                recall += 1.0 / (ref.length() - N + 1);
            }
        }
        return (1.0 + beta * beta) * precision * recall / (beta * beta * precision + recall);
    }
}
