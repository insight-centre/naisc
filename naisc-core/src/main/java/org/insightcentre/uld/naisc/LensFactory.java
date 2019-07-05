package org.insightcentre.uld.naisc;

import java.util.Map;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.util.Lazy;

/**
 * For creating lenses
 * @author John McCrae
 */
public interface LensFactory {
    /**
     * Make a lens
     * @param tag A tag associated with this lens (or null for no tag)
     * @param dataset The data containing the two elements
     * @param params Configuration parameters for this lens
     * @param analysis The analysis of the dataset
     * @return The lens
     */
    Lens makeLens(String tag, Dataset dataset, Map<String, Object> params,
            Lazy<Analysis> analysis);
}
