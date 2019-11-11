package org.insightcentre.uld.naisc.rescaling;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import org.insightcentre.uld.naisc.Rescaler;

/**
 * Rescale according to percentile
 * @author John McCrae
 */
public class Percentile implements Rescaler {

    @Override
    public double[] rescale(double[] value) {
        if(value.length <= 1) {
            return value;
        }
        double[] output = Arrays.copyOf(value, value.length);
        Arrays.sort(value);
        int[] freqs = dedupeFreq(value);
        if(freqs.length != value.length)
            value = Arrays.copyOf(value, freqs.length);
        int numerator = output.length - freqs[value.length - 1] + freqs[value.length - 2];
        for(int i = 0; i < output.length; i++) {
            int idx = Arrays.binarySearch(value, output[i]);
            output[i] = (double)freqs[idx] / numerator;
        }
        return output;
    }
    
    private static int[] dedupeFreq(double[] sorted) {
        int n = sorted.length;
        IntList freqs = new IntArrayList();
        int sum = 0;
        for (int i = 0; i < n - 1; i++) {
            if (sorted[i + 1] - sorted[i] < 1e-12) {
                int j = i + 1;
                for (; j < n && sorted[j] - sorted[i] < 1e-12; j++) {
                }
                if (j < sorted.length) {
                    System.arraycopy(sorted, j, sorted, i + 1, n - j);
                }
                n -= j - i - 1;
                freqs.add(sum);
                sum += j - i;
            } else {
                freqs.add(sum++);
            }
        }
        freqs.add(sum);
        return freqs.toIntArray();
    }

}
