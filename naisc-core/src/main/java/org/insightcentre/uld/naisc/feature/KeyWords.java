package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.PrettyGoodTokenizer;

/**
 * Keywords feature measures the Jaccard/Dice overlap of a set of key terms.
 * 
 * @author John McCrae
 */
public class KeyWords implements TextFeatureFactory {


    @Override
    public TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        List<String[]> keywords = new ArrayList<>();
        if(config.keywordsFile == null) {
            throw new ConfigurationException("Keywords file is required");
        }
        File keywordFile = new File(config.keywordsFile);
        if (!keywordFile.exists()) {
            throw new ConfigurationException("Keyword file does not exist");
        }
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(keywordFile));
            while ((line = br.readLine()) != null) {
                keywords.add(PrettyGoodTokenizer.tokenize(line.trim().toLowerCase()));
            }
        } catch (IOException x) {
            throw new ConfigurationException("Could not read keywords file", x);
        }
        return new KeyWordsImpl(keywords, tags);
    }

    /**
     * Configuration for keywords extraction.
     */
    public static class Configuration {

        /**
         * The file containing the key words. The file should contain the
         * keywords as a list, one per line
         */
        @ConfigurationParameter(description="The file containing the key words")
        public String keywordsFile;
    }

    private static class KeyWordsImpl implements TextFeature {

        private final List<String[]> keywords;
        private final Set<String> tags;

        public KeyWordsImpl(List<String[]> keywords, Set<String> tags) {
            this.keywords = keywords;
            this.tags = tags;
        }

        @Override
        public String id() {
            return "keywords";
        }

        private IntSet findKeywords(String sentence) {
            String[] tokens = PrettyGoodTokenizer.tokenize(sentence);
            IntSet matches = new IntOpenHashSet();
            for (int i = 0; i < keywords.size(); i++) {
                String[] keyword = keywords.get(i);
                int idx = Arrays.asList(tokens).indexOf(keyword[0]);
                LOOP:
                while (idx >= 0 && idx + keyword.length <= tokens.length) {
                    for (int j = idx + 1; j < idx + keyword.length; j++) {
                        if (!tokens[j].equals(keyword[j - idx])) {
                            idx = Arrays.asList(tokens).subList(idx +1, tokens.length).indexOf(keyword[0]) + idx + 1;
                            continue LOOP;
                        }
                    }
                    matches.add(i);
                    break;
                }
            }
            return matches;
        }

        @Override
        public Feature[] extractFeatures(LensResult lsp, NaiscListener log) {
            IntSet s1 = findKeywords(lsp.string1.toLowerCase());
            IntSet s2 = findKeywords(lsp.string2.toLowerCase());
            double A = s1.size();
            double B = s2.size();
            s1.retainAll(s2);
            double AB = s1.size();
            double dice = 2.0 * AB / (A + B);
            double jaccard = AB / (A + B - AB);
            return Feature.mkArray(new double[]{dice, jaccard}, getFeatureNames());
        }

        @Override
        public String[] getFeatureNames() {
            return new String[]{"keyword-dice", "keyword-jaccard"};
        }

        @Override
        public Set<String> tags() {
            return tags;
        }

        
        @Override
        public void close() throws IOException {
        }

    }

}
