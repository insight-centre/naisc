package org.insightcentre.uld.naisc.feature;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Essentially just loads a weighting map for each word, but tries to ensure that
 * each file is only loaded once
 * 
 * @author John McCrae
 */
public class WordWeighting {
    private static final Map<File, Object2DoubleMap<String>> weightings = new HashMap<>();
    private static final Object2IntMap<File> rc = new Object2IntOpenHashMap<>();

    public static Object2DoubleMap<String> get(File f)  throws IOException {
        if(weightings.containsKey(f)) {
            rc.put(f, rc.get(f) + 1);
            return weightings.get(f);
        } else {
            Object2DoubleMap saliency = new Object2DoubleRBTreeMap<>();
            try(DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
                int n = dis.readInt();
                for(int i = 0; i < n; i++) {
                    String s = dis.readUTF();
                    double d = dis.readDouble();
                    saliency.put(s, d);
                }
            }
            rc.put(f, 1);
            weightings.put(f, saliency);
            return saliency;
        }
    }
    
    public static void free(File f) {
        int rci = rc.getInt(f);
        if(rci <= 1) {
            weightings.remove(f);
            rc.remove(f);
        } else {
            rc.put(f, rci - 1);
        }
    }
    
}
