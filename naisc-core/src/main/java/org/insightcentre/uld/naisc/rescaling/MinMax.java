package org.insightcentre.uld.naisc.rescaling;

import org.insightcentre.uld.naisc.Rescaler;

public class MinMax implements Rescaler {
    @Override
    public double[] rescale(double[] value) {
        if(value.length <= 1)
            return value;

        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for(double d : value) {
            if(d < min) { min = d; }
            if(d > max) { max = d; }
        }
        if(!Double.isFinite(min) || !Double.isFinite(max))
            throw new RuntimeException("Could not apply min/max scaling as some values are infinite");
        if(max == min) {
            for(int i = 0; i < value.length; i++) {
                value[i] = 1;
            }
        } else {
            for(int i = 0; i < value.length; i++) {
                value[i] = (value[i] - min) / (max - min);
            }
        }
        return value;
    }
}
