package org.insightcentre.uld.naisc.feature.embeddings;

import static java.lang.Math.*;
import java.util.Arrays;


/**
 * Alignment-based features
 * 
 * @author John McCrae
 */
public class AlignmentFeatures {
    public static final double ALIGN_THRESH = 0.5;
    
    public static double forwardProp(WordAlignment alignment) {
        int n = 0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            double sum = 0.0;
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                sum += abs(alignment.alignment(i, j));
            }
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                if(abs(alignment.alignment(i, j)) / sum > ALIGN_THRESH) {
                    n++;
                    break;
                }
            }
        }
        if(alignment.getSourceSize() > 0) {
            return (double)n / alignment.getSourceSize();
        } else {
            return 0.0;
        }
    }
 
    public static double backProp(WordAlignment alignment) {
        int n = 0;
        for(int j = 0; j < alignment.getTargetSize(); j++) {
            double sum = 0.0;
            for(int i = 0; i < alignment.getSourceSize(); i++) {
                sum += abs(alignment.alignment(i, j));
            }
            for(int i = 0; i < alignment.getSourceSize(); i++) {
                if(abs(alignment.alignment(i, j)) / sum > ALIGN_THRESH) {
                    n++;
                    break;
                }
            }
        }
        if(alignment.getTargetSize() > 0) {
            return (double)n / alignment.getTargetSize();
        } else {
            return 0.0;
        }
    }

    public static double alignProportion(WordAlignment alignment) {
        int n1 = 0, n2 = 0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            double sum = 0.0;
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                sum += abs(alignment.alignment(i, j));
            }
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                if(abs(alignment.alignment(i, j)) / sum > ALIGN_THRESH) {
                    n1++;
                    break;
                }
            }
        }
        for(int j = 0; j < alignment.getTargetSize(); j++) {
            double sum = 0.0;
            for(int i = 0; i < alignment.getSourceSize(); i++) {
                sum += abs(alignment.alignment(i, j));
            }
            for(int i = 0; i < alignment.getSourceSize(); i++) {
                if(abs(alignment.alignment(i, j)) / sum > ALIGN_THRESH) {
                    n2++;
                    break;
                }
            }
        }
        return (double)(n1 + n2) / (alignment.getSourceSize() + alignment.getTargetSize());
    }

    
    public static double harmonicAlignmentMean(WordAlignment alignment) {
       double p1 = forwardProp(alignment);
       double p2 = backProp(alignment);
       if(p1 > 0.0 || p2 > 0.0) {
            return 2.0 * p1 * p2 / (p1 + p2);
       } else {
           return 0.0;
       }
    }
    
    public static double meanForwardVariance(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) {
            return 0.0;
        }
        double sumVar = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            double mu = 0.0;
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                mu += alignment.alignment(i, j);
            }
            mu /= alignment.getTargetSize();
            double var = 0.0;
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                double x = alignment.alignment(i, j) - mu;
                var += x * x;
            }
            var /= alignment.getTargetSize();
            if(mu != 0) {
                sumVar += sqrt(var) / mu;
            }
        }
        return sumVar / alignment.getSourceSize();
    }
    
    public static double meanBackVariance(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) {
            return 0.0;
        }
        double sumVar = 0.0;
        for(int j = 0; j < alignment.getTargetSize(); j++) {
            double mu = 0.0;
            for(int i = 0; i < alignment.getSourceSize(); i++) {
                mu += alignment.alignment(i, j);
            }
            mu /= alignment.getTargetSize();
            double var = 0.0;
            for(int i = 0; i < alignment.getSourceSize(); i++) {
                double x = alignment.alignment(i, j) - mu;
                var += x * x;
            }
            var /= alignment.getTargetSize();
            if(mu != 0.0) {
                sumVar += sqrt(var) / mu;
            }
        }
        return sumVar / alignment.getSourceSize();
    } 


    public static double dirichlet(WordAlignment alignment, double alpha) {
        double epsilon = 1e-9;
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) {
            return 0.0;
        }
        double sumD = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            double sum = 0.0;
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                sum += abs(alignment.alignment(i, j)) + epsilon;
            }
            if(sum > 0.0) {
                double d = 1.0;
                for(int j = 0; j < alignment.getTargetSize(); j++) {
                    d *= pow((abs(alignment.alignment(i, j)) + epsilon) / sum, alpha - 1);
                }
                sumD += d;// pow(d, 1.0 / alignment.getTargetSize());
                assert(!Double.isInfinite(sumD));
                assert(!Double.isNaN(sumD));
            }
        }
        return sumD / alignment.getSourceSize();
    }

    public static double maxStep(WordAlignment alignment) {
         if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) {
            return 0.0;
        }
        double sumStep = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            double[] vals = new double[alignment.getTargetSize()];
            for (int j = 0; j < alignment.getTargetSize(); j++) {
                vals[j] = alignment.alignment(i, j);
            }
            Arrays.sort(vals);
            if(vals[vals.length - 1] != vals[0]) {
                double step = 0;
                double sum = vals[vals.length - 1] - vals[0];
                for(int j = 1; j < vals.length; j++) {
                    double a = (vals[j] - vals[j - 1]) / sum;
                    if(a > step) {
                        step = a;
                    }
                }
                sumStep += step;
            }
        }
        return sumStep / alignment.getSourceSize();
    }

    public static double maxVal(WordAlignment alignment) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) {
            return 0.0;
        }
        double sumStep = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            double max = 0.0;
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                if(alignment.alignment(i, j) > max) {
                    max = alignment.alignment(i, j);
                }
            }
            sumStep += max;
        }
        return sumStep / alignment.getSourceSize();
    }

    public static double maxVal_p(WordAlignment alignment, double p) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) {
            return 0.0;
        }
        double sumStep = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            double max = 0.0;
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                double a = abs(alignment.alignment(i, j));
                if(a > max) {
                    max = a;
                }
            }
            sumStep += pow(max, p);
        }
        return pow(sumStep / alignment.getSourceSize(), 1.0 / p);
    }

    public static double col_lp(WordAlignment alignment, double p) {
        if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) {
            return 0.0;
        }
        double sumStep = 0.0;
        for(int i = 0; i < alignment.getSourceSize(); i++) {
            double colSum = 0.0;
            for(int j = 0; j < alignment.getTargetSize(); j++) {
                double a = abs(alignment.alignment(i, j));
                colSum += pow(a, p);
            }
            sumStep += pow(colSum / alignment.getTargetSize(), 1.0 /p);
        }
        assert(Double.isFinite(sumStep));
        return sumStep / alignment.getSourceSize();
    }
}
