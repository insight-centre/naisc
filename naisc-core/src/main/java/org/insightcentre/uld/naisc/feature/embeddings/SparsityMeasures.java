package org.insightcentre.uld.naisc.feature.embeddings;

import static java.lang.Math.*;
import java.util.Arrays;

/**
 * Sparsity Measures as proposed by 
 *   "Comparing Measures of Sparsity" - Hurley and Rickard (2009)
 * 
 * Some metrics modified by dividing by matrix size to ensure uniform output
 * 
 * @author John McCrae
 */
public class SparsityMeasures {
    private static final double EPS = 1e-9;
   
    public static double l0(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        int score = 0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                if(alignment.alignment(i, j) == 0) {
                    score++;
                }
            }
        }
        return (double)score / alignment.getSourceSize() / alignment.getTargetSize();
    }

    public static double l0e(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        final double e = (double)min(alignment.getSourceSize(), alignment.getTargetSize()) / 100;
        int score = 0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                if(abs(alignment.alignment(i, j)) < e) {
                    score++;
                }
            }
        }
        return (double)score / alignment.getSourceSize() / alignment.getTargetSize();
    }

    public static double l1(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double score = 0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                score += abs(alignment.alignment(i, j));
            }
        }
        return score / alignment.getSourceSize() / alignment.getTargetSize();
    }

    // Suggested value is p=0.5
    public static double lp(WordAlignment alignment, double p) {
        assert(p != 0);
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double score = 0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                score += pow(abs(alignment.alignment(i, j)), p);
            }
        }
        return pow(score / alignment.getSourceSize() / alignment.getTargetSize(), 1.0 / p);
    }

    public static double l2_over_l1(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double l2 = 0;
        double l1 = 0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                l2 += alignment.alignment(i, j) * alignment.alignment(i, j);
                l1 += abs(alignment.alignment(i, j));
            }
        }
        if(l1 != 0) {
            return sqrt(l2) / l1 / alignment.getSourceSize() / alignment.getTargetSize();
        } else {
            return 0.0;
        }
    }

    public static double tanh_ab(WordAlignment alignment, double a, double b) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double score = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                score += abs(tanh(pow(a * alignment.alignment(i, j), b)));
            }
        }
        return score / alignment.getSourceSize() / alignment.getTargetSize();
    }

    public static double neg_log(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double score = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                score += log(1 + alignment.alignment(i, j) * alignment.alignment(i, j));
            }
        }
        return score / alignment.getSourceSize() / alignment.getTargetSize();
    }
    
    public static double kappa4(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double l4 = 0.0;
        double l2 = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                double a = alignment.alignment(i, j);
                l4 += a*a*a*a;
                l2 += a*a;
            }
        }
        if(l2 != 0.0) {
            return l4 / l2 / l2 / alignment.getSourceSize() / alignment.getTargetSize();
        } else {
            return 0.0;
        }
    }

    // Change theta to step size
    public static double u_theta(WordAlignment alignment, int theta) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        if(alignment.getSourceSize() * alignment.getTargetSize() <= theta) {
            return 1.0;
        }
        double[] all = new double[alignment.getSourceSize() * alignment.getTargetSize()];
        int k = 0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                all[k++] = alignment.alignment(i, j);
            }
        }
        Arrays.sort(all);
        if(all[all.length - 1] == all[0]) {
            return 0.0;
        }
        double min_diff = Double.MAX_VALUE;
        for(int i = 0; i < all.length - theta; i++) {
            double diff = (all[i + theta] - all[i]) / (all[all.length - 1] - all[0]);
            if(diff < min_diff) {
                min_diff = diff;
            }
        }
        return 1.0 - min_diff;
    }

    public static double lpneg(WordAlignment alignment, double p) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double score = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                score += pow(abs(alignment.alignment(i, j)), p);
            }
        }
        return score / alignment.getSourceSize() / alignment.getTargetSize();
     }

    public static double Hg(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double score = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                double a = alignment.alignment(i, j);
                double l = log(alignment.alignment(i, j) != 0 ? a*a : EPS);
                score += l;
            }
        }
        
        assert(Double.isFinite(score));
        return score / alignment.getSourceSize() / alignment.getTargetSize();
    }

    public static double Hs(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double norm = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                norm += alignment.alignment(i, j) * alignment.alignment(i, j);
            }
        }
        if(norm == 0.0) {
            return 0.0;
        }
        double score = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                double c = alignment.alignment(i, j) * alignment.alignment(i, j) / norm;
                score += -c * log(c != 0 ? c : EPS);
            }
        }
        return score / alignment.getSourceSize() / alignment.getTargetSize();
    }

    public static double Hs2(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double score = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                double c = abs(alignment.alignment(i, j));
                score += -c * log(c != 0 ? c : EPS);
            }
        }
        return score / alignment.getSourceSize() / alignment.getTargetSize();
    }

    public static double hoyer(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double N = alignment.getSourceSize() * alignment.getTargetSize();
        if(N == 1) {
            return 1.0;
        }
        double c1 = 0.0;
        double c2 = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                c1 += abs(alignment.alignment(i, j));
                c2 += alignment.alignment(i, j) * alignment.alignment(i, j);
            }
        }
        if(c2 == 0) {
            return 0.0;
        } else {
            return (sqrt(N) - c1 / (sqrt(c2))) / (sqrt(N) - 1);
        }
    }

    public static double gini(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) { return 0.0; }
        double[] all = new double[alignment.getSourceSize() * alignment.getTargetSize()];
        double c1 = 0.0;
        int k = 0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                c1 += abs(alignment.alignment(i, j));
                all[k++] = abs(alignment.alignment(i, j));
            }
        }
        if(c1 == 0.0) {
            return 0;
        }
        Arrays.sort(all);
        double score = 0.0;
        double N = alignment.getSourceSize() * alignment.getTargetSize();
        for(int i = 0; i < all.length; i++) {
            score += abs(all[i]) / c1 * (N - i + 0.5) / N;
        }
        return 1 - 2.0 * score;
    }
}
