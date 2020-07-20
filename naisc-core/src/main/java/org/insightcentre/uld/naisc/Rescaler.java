package org.insightcentre.uld.naisc;

import it.unimi.dsi.fastutil.doubles.DoubleList;

/**
 * Rescales the results according to some sensible principle
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public interface Rescaler {
    
    /**
     * Take a list of values and provide a rescaling
     * @param value The input (unscaled) values
     * @return The rescaled values
     */
    double[] rescale(double[] value);
}
