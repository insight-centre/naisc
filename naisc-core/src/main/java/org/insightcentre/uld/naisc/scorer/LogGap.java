package org.insightcentre.uld.naisc.scorer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.function.Function;
import org.insightcentre.uld.naisc.ScoreResult;

/**
 * Provides a normalization of the values in a sequence, such that when sorted
 * the gap between values is proportional to the log of the gap between the
 * input.
 *
 * @author John McCrae
 */
public class LogGap {

    public static final int MAX_VALUES = 10000;
    private final DoubleList valuesBuilder = new DoubleArrayList();
    private double[] values = null;
    private double[] diffs = null;
    private double sumdiff = 0.0;
    private boolean complete = false;

    /**
     * Create a new log gap that has not seen any data
     */
    public LogGap() {
    }

    private LogGap(double[] values, double[] diffs, double sumdiff) {
        this.values = values;
        this.diffs = diffs;
        this.sumdiff = sumdiff;
        this.complete = true;
    }

    public LogGapModel toModel() {
        return new LogGapModel(values, diffs, sumdiff);
    }

    public LogGapModel toModel(int maxSize) {
        if(values == null) {
            makeModel(valuesBuilder.toDoubleArray());
        }
        if(values.length < maxSize) {
            return this.toModel();
        } else {
            double[] v = new double[maxSize];
            double[] d = new double[maxSize];
            double n = (double)values.length / maxSize;
            for(int i = 0; i < maxSize; i++) {
                v[i] = values[values.length * i / maxSize];
                d[i] = diffs[values.length * i / maxSize];
            }
            v[maxSize - 1] = values[values.length - 1];
            d[maxSize - 1] = diffs[diffs.length - 1];
            return new LogGapModel(v, d, sumdiff);
        }
    }

    public static LogGap fromModel(LogGapModel model) {
        return new LogGap(model.values, model.diffs, model.sumdiff);
    }

    public boolean isComplete() {
        return complete || valuesBuilder.size() >= MAX_VALUES;
    }

    public void addResult(double d) {
        if (valuesBuilder.size() < MAX_VALUES && Double.isFinite(d)) {
            valuesBuilder.add(d);
        }
    }

    public ScoreResult result(double d, String relation) {
        return new ScoreResult(normalize(d), relation);
    }

    public static double[] dedupe(double[] sorted) {
        int n = sorted.length;
        for (int i = 0; i < n - 1; i++) {
            if (sorted[i + 1] - sorted[i] < 1e-12) {
                int j = i + 1;
                for (; j < n && sorted[j] - sorted[i] < 1e-12; j++) {
                }
                if (j < sorted.length) {
                    System.arraycopy(sorted, j, sorted, i + 1, n - j);
                }
                n -= j - i - 1;
            }
        }
        if (n != sorted.length) {
            return Arrays.copyOf(sorted, n);
        } else {
            return sorted;
        }
    }

    public void makeModel(double[] _values) {
        _values = removeNaNs(_values);
        Arrays.sort(_values);
        this.values = dedupe(_values);
        this.diffs = new double[values.length];
        this.sumdiff = 0;
        for (int i = 0; i < values.length - 1; i++) {
            diffs[i + 1] = Math.log(values[i + 1] - values[i] + 1);
            if (!Double.isFinite(diffs[i + 1])) {
                throw new RuntimeException("values too close " + values[i + 1] + " - " + values[i] + "=" + (values[i + 1] - values[i]));
            }
            sumdiff += diffs[i + 1];
        }
        if (sumdiff == 0.0) { // i.e., all values are the same
            sumdiff = 1.0;
        }
        for (int i = 1; i < values.length; i++) {
            diffs[i] = diffs[i - 1] + diffs[i] / sumdiff;
        }
        valuesBuilder.clear();
    }

    /**
     * Strip all NaN values from a vector and return only the non-NaN values
     */
    public static double[] removeNaNs(double[] values) {
        int nanCount = 0;
        for(int i = 0; i < values.length - nanCount; i++) {
            if(Double.isNaN(values[i])) {
                values[i] = values[values.length - 1 - nanCount];
                nanCount++;
                i--;
            }
        }
        if(nanCount > 0) {
            return Arrays.copyOf(values, values.length - nanCount);
        } else {
            return values;
        }
    }

    public double normalize(double d) {
        if (values == null) {
            makeModel(valuesBuilder.toDoubleArray());
        }
        if (values.length <= 1) {
            return d;
        }
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
            int n = 1;
            int m = 0;
            while (a + b == 0) {
                if ((n > m || i - n <= 0) && i + m + 1 < values.length) {
                    m++;
                    b = Math.log(values[i + m] - d + 1);
                } else if (i - n > 0) {
                    n++;
                    a = Math.log(d - values[i - n] + 1);
                } else {
                    throw new RuntimeException("Could not create a non-zero window");
                }
            }
            double c = Math.log(values[i + m] - values[i - n] + 1);
            return diffs[i - n] + c * a / (a + b) / sumdiff;
        }
    }

    /*private class LogGapResult implements ScoreResult {

        private final double d;
        private final String relation;

        public LogGapResult(double d, String relation) {
            this.d = d;
            this.relation = relation;
        }

        @Override
        public double value() {
            if (values == null) {
                makeModel(valuesBuilder.toDoubleArray());
            }
            return normalize(d);
        }

        @Override
        public String relation() {
            return relation;
        }
    }*/

    public static class LogGapModel {
        public final double[] values;
        public final double[] diffs;
        public final double sumdiff;

        /**
         * Create a log gap model
         */
         @JsonCreator
        public LogGapModel(@JsonProperty("values") double[] values, @JsonProperty("diffs") double[] diffs, @JsonProperty("sumdiff") double sumdiff) {
            this.values = values;
            this.diffs = diffs;
            this.sumdiff = sumdiff;
        }
    }
}
