package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.feature.mt.*;
import org.insightcentre.uld.naisc.util.PrettyGoodTokenizer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A group of metrics for string similarity derived from machine translation
 */
public class MachineTranslation implements TextFeatureFactory  {
    @Override
    public TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        return new MachineTranslationImpl(config, tags);
    }

    public enum Method {
        BLEU,
        BLEU2,
        chrF,
        METEOR,
        NIST,
        TER
    }

    /**
     * Configuration for machine translation metrics
     */
    @ConfigurationClass("String similarity methods based on those widely-used for the evaluation of machine translation")
    public static class Configuration {
        /**
         * The methods to use
         */
         @ConfigurationParameter(description = "The methods to use", defaultValue = "[\"BLEU\", \"BLEU-2\", \"chrF\", \"METEOR\", \"NIST\", \"TER\"]")
        public List<Method> methods = Arrays.asList(Method.BLEU, Method.BLEU2, Method.chrF, Method.METEOR, Method.NIST, Method.TER);
        @ConfigurationParameter(description = "The n-gram to use for BLEU", defaultValue = "4")
        public int bleuN = 4;
        @ConfigurationParameter(description = "The n-gram to use for the second BLEU", defaultValue = "2")
        public int bleuN2 = 2;
        @ConfigurationParameter(description = "The n-gram size for chrF", defaultValue = "6")
        public int chrFN = 6;
        @ConfigurationParameter(description = "The beat paramater for chrF", defaultValue = "3")
        public int chrFbeta = 3;
        @ConfigurationParameter(description = "The n-gram size for NIST", defaultValue = "4")
        public int nistN = 4;
    }

    private static class MachineTranslationImpl implements TextFeature {
        private final Configuration config;
        private final Set<String> tags;

        public MachineTranslationImpl(Configuration config, Set<String> tags) {
            this.config = config;
            this.tags = tags;
        }

        @Override
        public String id() {
            return "mt";
        }

        @Override
        public Feature[] extractFeatures(LensResult facet, NaiscListener log) {
            Feature[] features = new Feature[config.methods.size()];
            for(int i = 0; i < config.methods.size(); i++) {
                String[] string1tok = PrettyGoodTokenizer.tokenize(facet.string1);
                String[] string2tok = PrettyGoodTokenizer.tokenize(facet.string2);
                switch(config.methods.get(i)) {
                    case BLEU:
                        features[i] = new Feature("BLEU", BLEU.bleuScore(string1tok, string2tok, config.bleuN));
                        break;
                    case BLEU2:
                        features[i] = new Feature("BLEU2", BLEU.bleuScore(string1tok, string2tok, config.bleuN2));
                        break;
                    case chrF:
                        features[i] = new Feature("chrF", chrF.chrF(facet.string1, facet.string2, config.chrFN, config.chrFbeta));
                        break;
                    case METEOR:
                        features[i] = new Feature("METEOR", METEOR.meteorScore(facet.string1, facet.string2));
                        break;
                    case NIST:
                        features[i] = new Feature("NIST", NIST.nistScore(string1tok, string2tok, config.nistN));
                        break;
                    case TER:
                        features[i] = new Feature("TER", TER.terScore(facet.string1, facet.string2));
                        break;
                    default:
                        throw new RuntimeException("Unreachable");
                }
            }
            return features;
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
