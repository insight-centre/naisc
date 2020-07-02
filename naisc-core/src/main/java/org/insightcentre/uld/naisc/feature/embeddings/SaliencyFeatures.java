package org.insightcentre.uld.naisc.feature.embeddings;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.io.File;
import java.io.IOException;
import org.insightcentre.uld.naisc.feature.WordWeighting;

/**
 * Saliency based features
 * 
 * @author John McCrae
 */
public class SaliencyFeatures {

    private static SaliencyFeatures instance;
    private final Object2DoubleMap<String> saliency;

    private SaliencyFeatures() {
        this(new File("models/saliency"));
    }

    public SaliencyFeatures(File f) {
        try {
            saliency = WordWeighting.get(f);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
                
    }

    public static SaliencyFeatures instance() { 
        if(instance == null) { 
            instance = new SaliencyFeatures();
        } 
        return instance;
    }
    
    public SaliencyFeatures(Object2DoubleMap<String> saliency) {
        this.saliency = saliency;
    }

    public double maxSaliencyWt(WordAlignment alignment) {
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
            final String w = alignment.getSourceSentence().tokens[i];
            //sumStep += max * (saliency.containsKey(w) ? Math.log10(1.0 / saliency.getDouble(w)) : 5.0);
            sumStep += max * (saliency.containsKey(w) ? 1.0 - saliency.getDouble(w) : 1.0);
        }
        return sumStep / alignment.getSourceSize();
    }

    public double maxSaliencyWtInv(WordAlignment alignment) {
       if(alignment.getSourceSize() == 0 || alignment.getTargetSize() == 0) {
            return 0.0;
        }
        double sumStep = 0.0;
        for(int j = 0; j < alignment.getTargetSize(); j++) {
            double max = 0.0;
            for(int i = 0; i < alignment.getSourceSize(); i++) {
                if(alignment.alignment(i, j) > max) {
                    max = alignment.alignment(i, j);
                }
            }
            final String w = alignment.getTargetSentence().tokens[j];
            //sumStep += max * (saliency.containsKey(w) ? Math.log10(1.0 / saliency.getDouble(w)) : 5.0);
            sumStep += max * (saliency.containsKey(w) ? 1.0 - saliency.getDouble(w) : 1.0);
        }
        return sumStep / alignment.getTargetSize();
    }    
}
