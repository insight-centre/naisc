package org.insightcentre.uld.naisc.rescaling;

import java.util.Arrays;
import org.insightcentre.uld.naisc.Rescaler;
import org.insightcentre.uld.naisc.scorer.LogGap;

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
        value = LogGap.dedupe(value);
        for(int i = 0; i < output.length; i++) {
            int idx = Arrays.binarySearch(value, output[i]);
            output[i] = (double)idx / (value.length - 1);
        }
        return output;
    }

}
