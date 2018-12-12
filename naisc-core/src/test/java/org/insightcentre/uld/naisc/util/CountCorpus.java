package org.insightcentre.uld.naisc.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Count a corpus to provide weighting files. Example usage
 * 
 * mvn exec:java -Dexec.mainClass="org.insightcenter.uld.naisc.util.CountCorpus" -Dexec.args="models/wiki.en.gz"
 * @author John McCrae <john@mccr.ae>
 */
public class CountCorpus {

    public static void main(String[] args) throws Exception {
        final Object2IntMap<String> wordMap = new Object2IntRBTreeMap<>();
        final Object2IntMap<String> ngMap = new Object2IntRBTreeMap<>();

        int lines = 0;

        try(BufferedReader reader = new BufferedReader(args[0].endsWith(".gz") ? new InputStreamReader(new GZIPInputStream(new FileInputStream(args[0]))) : new FileReader(args[0]))) {
            String line = null;
            while((line = reader.readLine()) != null) {
                String[] wordsAll = line.split(" ");
                ObjectSet<String> words = new ObjectRBTreeSet<>(wordsAll);
                for(String word : words) {
                    wordMap.put(word, wordMap.getInt(word) + 1);
                }

                ObjectSet<String> ngs = new ObjectRBTreeSet<>();
                for(int n = 2; n < 5; n++) {
                    for(int i = 0; i < line.length() - n; i++) {
                        String ng = line.substring(i, i + n);
                        ngs.add(ng);
                    }
                }

                for(String ng : ngs) {
                    ngMap.put(ng, ngMap.getInt(ng) + 1);
                }

                if(++lines % 1000 == 0) {
                    System.err.print(".");
                }
            }
        }

        System.err.println("Saving");
        try(DataOutputStream dos = new DataOutputStream(new FileOutputStream("models/idf"))) {
            dos.writeInt(wordMap.size());
            for(Object2IntMap.Entry<String> e : wordMap.object2IntEntrySet()) {
                dos.writeUTF(e.getKey());
                dos.writeDouble((double)e.getIntValue() / lines);
            }
        }

        try(DataOutputStream dos = new DataOutputStream(new FileOutputStream("models/ngidf"))) {
            dos.writeInt(ngMap.size());
            for(Object2IntMap.Entry<String> e : ngMap.object2IntEntrySet()) {
                dos.writeUTF(e.getKey());
                dos.writeDouble((double)e.getIntValue() / lines);
            }
        }
        
    }
}
