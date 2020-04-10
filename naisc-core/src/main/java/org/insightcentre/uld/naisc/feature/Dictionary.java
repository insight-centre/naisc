package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.StringPair;

/**
 * Simple check if a term is already a synonym in a dictionary
 * @author John McCrae
 */
public class Dictionary implements TextFeatureFactory {
    /** Configuration for basic.dict.Dictionary */
    public static class Configuration {
        /** The dictionary to use */
        @ConfigurationParameter(description = "The dictionary to use")
        public String dict;
    }

    @Override
    public TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if(config.dict != null) {
            File f = new File(config.dict);
            if(!f.exists()) {
                throw new IllegalArgumentException("Dict does not exist: " + f);
            }
            try(final BufferedReader reader = openFile(f)) {
                final Set<StringPair> dictionary = new HashSet<>();
                String line;
                while((line = reader.readLine()) != null) {
                    if(line.isEmpty())
                        continue;
                    String[] elems = line.split("\t");
                    if(elems.length != 2) 
                        throw new IllegalArgumentException("Bad line in dict: " + line);
                    dictionary.add(new StringPair(elems[0], elems[1]));
                }
                return new DictionaryFeatureExtractor(dictionary, tags);
            } catch(IOException x) {
                throw new IllegalArgumentException("Could not read dict", x);
            }
        } else {
            throw new IllegalArgumentException("dict is a required parameter");
        }
    }

    private BufferedReader openFile(File f) throws IOException {
        BufferedReader reader;
        if(f.getName().endsWith(".gz")) {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
        } else {
            reader = new BufferedReader(new FileReader(f));
        }
        return reader;
    }
    
    private static class DictionaryFeatureExtractor implements TextFeature {
        private final Set<StringPair> dictionary;
        private final Set<String> tags;

        public DictionaryFeatureExtractor(Set<StringPair> dictionary, Set<String> tags) {
            this.dictionary = dictionary;
            this.tags = tags;
        }

        @Override
        public Set<String> tags() {
            return tags;
        }

        @Override
        public String id() {
            return "dict";
        }

        @Override
        public Feature[] extractFeatures(LensResult lsp, NaiscListener log) {
            StringPair sp1 = new StringPair(lsp.string1, lsp.string2);
            StringPair sp2 = new StringPair(lsp.string2, lsp.string1);
            return Feature.mkArray(new double[] {
                dictionary.contains(sp1) || dictionary.contains(sp2) ? 1.0 : 0.0
            }, featName);
        }

        private static final String[] featName = new String[] { "dict" };
        @Override
        public String[] getFeatureNames() {
            return featName;
        }

        @Override
        public void close() throws IOException {
        }

    }
}
