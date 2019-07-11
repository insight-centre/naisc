package org.insightcentre.uld.naisc.scorer;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import org.insightcentre.uld.naisc.ScoreResult;

/**
 * Provides a normalization of the values in a sequence, such that when sorted
 * the gap between values is proportional to the log of the gap between the input.
 * 
 * @author John McCrae
 */
public class LogGap {

    private final DoubleList valuesBuilder = new DoubleArrayList();
    private double[] values = null;
    private double[] diffs = null;
    private double sumdiff = 0.0;

    public ScoreResult result(double d) {
        valuesBuilder.add(d);
        if(values != null) {
            values = null;
            diffs = null;
        }
        return new LogGapResult(d);
    }

    public void makeModel(double[] values) {
        this.values = values;
        this.diffs = new double[values.length];
        Arrays.sort(values);
        this.sumdiff = 0;
        for (int i = 0; i < values.length - 1; i++) {
            diffs[i + 1] = Math.log(values[i + 1] - values[i] + 1);
            sumdiff += diffs[i + 1];
        }
        for (int i = 1; i < values.length; i++) {
            diffs[i] = diffs[i - 1] + diffs[i] / sumdiff;
        }
    }

    public double normalize(double d) {
        if(values.length <= 1)
            return d;
        int i = Arrays.binarySearch(values, d);
        if (i >= 0) {
            return diffs[i];
        }
        i = -i - 1;
        if (i == 0) {
            return -Math.log(values[0] - d + 1) / sumdiff;
        } else if (i >= values.length) {
            return 1 + Math.log(d - values[values.length - 1] + 1) / sumdiff;
        } else {
            double a = Math.log(d - values[i - 1] + 1);
            double b = Math.log(values[i] - d + 1);
            double c = Math.log(values[i] - values[i - 1] + 1);
            return diffs[i - 1] + c * a / (a + b) / sumdiff;
        }
    }

    private class LogGapResult implements ScoreResult {
        private final double d;

        public LogGapResult(double d) {
            this.d = d;
        }
        
        @Override
        public double value() {
            if(values == null) {
                makeModel(valuesBuilder.toDoubleArray());
            }
            return normalize(d);
        }
        
    }
}
