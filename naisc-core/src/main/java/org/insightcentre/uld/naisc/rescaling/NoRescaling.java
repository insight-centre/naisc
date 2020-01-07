package org.insightcentre.uld.naisc.rescaling;

import org.insightcentre.uld.naisc.Rescaler;

public class NoRescaling implements Rescaler{
    @Override
    public double[] rescale(double[] value) {
        return value;
    }
}
