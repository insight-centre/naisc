package org.insightcentre.uld.naisc.feature.embeddings;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the standard WAFE
 * @author John McCrae
 */
public class StandardWordAlignmentFeatureExtractor {
    private final String[] features;
    private final SaliencyFeatures saliencyFeatures;
    public static final String[] DEFAULT_FEATURES = new String[] {"fp", "bp", "ham", "max", "max2", "max.5", 
            "max.1", "collp2", "collp10", "Hg", "salmax", "salmaxi" };

    public StandardWordAlignmentFeatureExtractor() {
        this.features = DEFAULT_FEATURES;
        this.saliencyFeatures = SaliencyFeatures.instance();
            
    }

    public StandardWordAlignmentFeatureExtractor(List<String> features, SaliencyFeatures saliencyFeatures) {
        this.features = features.toArray(new String[features.size()]);
        this.saliencyFeatures = saliencyFeatures;
    }
    
    
    public double[] makeFeatures(WordAlignment alignment) {
        double[] values = new double[features.length];
        if(alignment.isEmpty()) {
            Arrays.fill(values, 0.0);
        } else {
            int i = 0;
            for(String s : features) {
                switch(s) {
                    case "fp":
                        values[i++] = AlignmentFeatures.forwardProp(alignment);
                        break;
                    case "bp":
                        values[i++] = AlignmentFeatures.backProp(alignment);
                        break;
                    case "ap":
                        values[i++] = AlignmentFeatures.alignProportion(alignment);
                        break;
                    case "ham":
                        values[i++] = AlignmentFeatures.harmonicAlignmentMean(alignment);
                        break;
                    case "max":
                        values[i++] = AlignmentFeatures.maxVal(alignment);
                        break;
                    case "max2":
                        values[i++] = AlignmentFeatures.maxVal_p(alignment, 2);
                        break;
                    case "max.5":
                        values[i++] = AlignmentFeatures.maxVal_p(alignment, .2);
                        break;
                    case "max.1":
                        values[i++] = AlignmentFeatures.maxVal_p(alignment, .1);
                        break;
                    case "collp2":
                        values[i++] = AlignmentFeatures.col_lp(alignment, 2);
                        break;
                    case "collp10":
                        values[i++] = AlignmentFeatures.col_lp(alignment, 10);
                        break;
                    case "Hg":
                        values[i++] = SparsityMeasures.Hg(alignment);
                        break;
                    case "salmax":
                        values[i++] = saliencyFeatures.maxSaliencyWt(alignment);
                        break;
                    case "salmaxi":
                        values[i++] = saliencyFeatures.maxSaliencyWtInv(alignment);
                        break;
                    default:
                        throw new RuntimeException("Unsupported feature: " + s);
                }
            }
        }

        return values;
    }

    public String[] featureNames() {
        return features;
    }
    
}
