package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;

import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.feature.wordnet.GWNWordNetReader;
import org.insightcentre.uld.naisc.feature.wordnet.SemanticSimilarityMeasures;
import org.insightcentre.uld.naisc.feature.wordnet.Synset;
import org.insightcentre.uld.naisc.feature.wordnet.WordNetData;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.PrettyGoodTokenizer;
import org.xml.sax.SAXException;

/**
 * Calculate the similarity of two string using WordNet based similarity
 *
 * @author John McCrae
 */
public class WordNet implements TextFeatureFactory {

    public enum Method {
        SHORTEST_PATH,
        WU_PALMER,
        LEAKCOCK_CHODOROW,
        LI
    };

    @Override
    public TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        File wordnetXmlFile = new File(config.wordnetXmlFile);
        if(!wordnetXmlFile.exists()) {
            throw new ConfigurationException("WordNet XML file does not exist");
        }
        try {
            WordNetData wordnet = GWNWordNetReader.readFile(wordnetXmlFile);
            return new WordNetImpl(tags, wordnet, config.methods);
        } catch(IOException|SAXException|ParserConfigurationException x) {
            throw new ConfigurationException("Could not load WordNet from XML", x);
        }
    }

    /**
     * Configuration for WordNet similarity.
     */
    public static class Configuration {
        /**
         * The path to the WordNet file in GWA XML format.
         */
        @ConfigurationParameter(description = "The path to the WordNet file in GWA XML format")
        public String wordnetXmlFile;
        /**
         * The methods to use. Should not be empty
         */
        @ConfigurationParameter(description = "The methods to use")
        public List<Method> methods = Arrays.asList(Method.SHORTEST_PATH, Method.LEAKCOCK_CHODOROW, Method.LI, Method.WU_PALMER);

    }

    private static class WordNetImpl implements TextFeature {

        private final Set<String> tags;
        private final WordNetData wordnet;
        private final List<Method> methods;
        private final SemanticSimilarityMeasures ssm;

        public WordNetImpl(Set<String> tags, WordNetData wordnet, List<Method> methods) {
            this.tags = tags;
            this.wordnet = wordnet;
            this.methods = methods;
            this.ssm = new SemanticSimilarityMeasures(wordnet);
        }

        public List<String[]> findSynsets(String[] tokens) {
            return new ArrayList<>(wordnet.allEntries(tokens).keySet());
        }

        public double[] score(String left, String right, Method method) {
            List<String[]> lwords = findSynsets(PrettyGoodTokenizer.tokenize(left));
            List<String[]> rwords = findSynsets(PrettyGoodTokenizer.tokenize(right));
            if (lwords.isEmpty() || rwords.isEmpty()) {
                return new double[] { 0.0, 0.0 };
            }
            double total1 = 0.0;
            for (String[] lword : lwords) {
                double score = Double.NEGATIVE_INFINITY;
                for (String[] rword : rwords) {
                    double s = sim(lword, rword, method);
                    if (s > score) {
                        score = s;
                    }
                }
                total1 += score;
            }
            double total2 = 0.0;
            for (String[] rword : rwords) {
                double score = Double.NEGATIVE_INFINITY;
                for (String[] lword : lwords) {
                    double s = sim(lword, rword, method);
                    if (s > score) {
                        score = s;
                    }
                }
                total2 += score;
            }
            return new double[] { total1 / lwords.size(), total2 / rwords.size() };
        }

        @Override
        public String id() {
            return "wordnet";

        }

        @Override
        public Feature[] extractFeatures(LensResult facet, NaiscListener log) {
            double[] vec = new double[methods.size() * 2];
            int i = 0;
            for(Method method : methods) {
                System.arraycopy(score(facet.string1, facet.string2, method), 0, vec, i, 2);
                i += 2;
            }
            return Feature.mkArray(vec, getFeatureNames());

        }

        @Override
        public String[] getFeatureNames() {
            String[] vec = new String[methods.size() * 2];
            int i = 0;
            for(Method method : methods) {
                vec[i++] = method.toString().toLowerCase() + "-left";
                vec[i++] = method.toString().toLowerCase() + "-right";
            }
            return vec;
        }

        @Override
        public Set<String> tags() {
            return tags;

        }

        @Override
        public void close() throws IOException {
        }

        private double sim(String[] lword, String[] rword, Method method) {
            
            List<Synset> ls = wordnet.lookupEntry(lword).stream().flatMap(x -> x.synsets(wordnet).stream()).collect(Collectors.toList());
            List<Synset> rs = wordnet.lookupEntry(rword).stream().flatMap(x -> x.synsets(wordnet).stream()).collect(Collectors.toList());
            if(ls.isEmpty() || rs.isEmpty())
                return 0.0;
            switch(method) {
                case LEAKCOCK_CHODOROW:
                    return 1.0 - ls.stream().flatMap(l -> {
                        return rs.stream().map(r -> ssm.leakcockChodorow(l, r));
                    }).max(Comparator.naturalOrder()).get();
                case LI:
                    return 1.0 - ls.stream().flatMap(l -> {
                        return rs.stream().map(r -> ssm.li(l, r));
                    }).max(Comparator.naturalOrder()).get();
                case SHORTEST_PATH:
                    return 1.0 - ls.stream().flatMap(l -> {
                        return rs.stream().map(r -> ssm.shortestPath(l, r));
                    }).max(Comparator.naturalOrder()).get();
                case WU_PALMER:
                    return 1.0 - ls.stream().flatMap(l -> {
                        return rs.stream().map(r -> ssm.wuPalmer(l, r));
                    }).max(Comparator.naturalOrder()).get();
            }
            throw new RuntimeException("Method was null");
        }

    }

}
