package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import static eu.monnetproject.lang.Language.ENGLISH;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.io.File;
import java.io.IOException;

import static java.lang.Integer.min;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.text.similarity.SimilarityScore;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.PrettyGoodTokenizer;

/**
 * The basic features of string similarity
 *
 * @author John McCrae
 */
public class BasicString implements TextFeatureFactory {

    private static final SimilarityScore<Double> JARO_WINKLER = new JaroWinklerSimilarity();
    private static final LevenshteinDistance LEVENSHTEIN = new LevenshteinDistance();

    /**
     * The features implemented by basic string
     */
    public enum Feature {
        lcs,
        lc_prefix,
        lc_suffix,
        ngram_1,
        ngram_2,
        ngram_3,
        ngram_4,
        ngram_5,
        jaccard,
        dice,
        containment,
        senLenRatio,
        aveWordLenRatio,
        negation,
        number,
        jaroWinkler,
        levenshtein,
        mongeElkanJaroWinkler,
        mongeElkanLevenshtein
    }

    /**
     * Configuration for basic.BasicString
     */
    public static class Configuration {

        /**
         * Also extract character-level features
         */
        @ConfigurationParameter(description = "Also extract character-level features")
        public boolean labelChar;
        /**
         * Weight the words according to this file
         */
        @ConfigurationParameter(description = "Weight the words according to this file")
        public String wordWeights;
        /**
         * Weight the character n-grams according to this file
         */
        @ConfigurationParameter(description = "Weight the character n-grams according to this file")
        public String ngramWeights;
        /**
         * Use only features with the given names
         */
        @ConfigurationParameter(description = "The features to extract", defaultValue = "null")
        public List<Feature> features;
        /**
         * Convert all strings to lower case before processing
         */
        @ConfigurationParameter(description = "Convert all strings to lower case before processing", defaultValue = "true")
        public boolean lowerCase = true;

        public Object2DoubleMap<String> ngramWeights() {
            if (ngramWeights != null && !"".equals(ngramWeights)) {
                try {
                    final File file = new File(ngramWeights);
                    if (!file.exists()) {
                        throw new RuntimeException("Could not find file: " + file.getAbsolutePath());
                    }
                    return WordWeighting.get(file);
                } catch (IOException x) {
                    throw new RuntimeException(x);
                }
            } else {
                return null;
            }
        }

        public Object2DoubleMap<String> wordWeights() {
            if (wordWeights != null && !"".equals(wordWeights)) {
                try {
                    final File file = new File(wordWeights);
                    if (!file.exists()) {
                        throw new RuntimeException("Could not find file: " + file.getAbsolutePath());
                    }
                    return WordWeighting.get(file);
                } catch (IOException x) {
                    throw new RuntimeException(x);
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params) {
        final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Configuration config = mapper.convertValue(params, Configuration.class);
        return new BasicStringImpl(config.labelChar, config.wordWeights(),
                config.ngramWeights(),
                tags,
                config.features == null ? null : new HashSet<>(config.features),
                config.lowerCase);
    }

    static class BasicStringImpl implements TextFeature {

        private final boolean labelCharFeatures;
        private final NGramWeighting ngramWeighting, wordWeighting;
        private final Set<String> tag;
        private final Set<Feature> selectedFeatures;
        private final boolean lowerCase;

        /**
         * The basic classifier features
         *
         * @param labelCharFeatures Also generate features for the label based
         * on character level similarity
         * @param wordWeights The weighting to give to each word
         * @param ngramWeights The weighting to give to each ngram
         * @param tag The tag if any to use
         * @param features Which features to use or null for all
         * @param lowerCase Lower case the input
         */
        public BasicStringImpl(boolean labelCharFeatures,
                Object2DoubleMap<String> wordWeights,
                Object2DoubleMap<String> ngramWeights,
                Set<String> tag,
                Set<Feature> features,
                boolean lowerCase) {
            this.labelCharFeatures = labelCharFeatures;
            if (wordWeights != null) {
                wordWeighting = new SumWeighting(wordWeights);
            } else {
                wordWeighting = new ConstantWeighting();
            }
            if (ngramWeights != null) {
                ngramWeighting = new CatWeighting(ngramWeights);
            } else {
                ngramWeighting = new ConstantWeighting();
            }
            this.tag = tag;
            this.selectedFeatures = features == null || features.isEmpty() ? null : features;
            this.lowerCase = lowerCase;
        }

        @Override
        public String id() {
            return "basic-features";
        }

        @Override
        public Set<String> tags() {
            return tag;
        }

        private String[] _featureNames;

        @Override
        public String[] getFeatureNames() {
            if (_featureNames != null) {
                return _featureNames;
            } else {
                ArrayList<String> fn = new ArrayList<>();
                buildFeatureNames("", fn, false);
                if (labelCharFeatures) {
                    buildFeatureNames("char-", fn, true);
                }
                return _featureNames = fn.toArray(new String[fn.size()]);
            }
        }

        private void buildFeatureNames(String prefix, ArrayList<String> featureNames, boolean charLevel) {
            if (selectedFeatures == null || selectedFeatures.contains(Feature.lcs)) {
                featureNames.add(prefix + "lcs");
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.lc_suffix)) {
                featureNames.add(prefix + "lc_suffix");
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.lc_prefix)) {
                featureNames.add(prefix + "lc_prefix");
            }
            if (!charLevel) {
                if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_1)) {
                    featureNames.add(prefix + "ngram-1");
                }
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_2)) {
                featureNames.add(prefix + "ngram-2");
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_3)) {
                featureNames.add(prefix + "ngram-3");
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_4)) {
                featureNames.add(prefix + "ngram-4");
            }
            if (charLevel) {
                if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_5)) {
                    featureNames.add(prefix + "ngram-5");
                }
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.jaccard)) {
                featureNames.add(prefix + "jaccard");
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.dice)) {
                featureNames.add(prefix + "dice");
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.containment)) {
                featureNames.add(prefix + "containment");
            }

            if (!charLevel) {
                if (selectedFeatures == null || selectedFeatures.contains(Feature.senLenRatio)) {
                    featureNames.add(prefix + "senLenRatio");
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.aveWordLenRatio)) {
                    featureNames.add(prefix + "aveWordLenRatio");
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.negation)) {
                    featureNames.add(prefix + "negation");
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.number)) {
                    featureNames.add(prefix + "number");
                }
            }

            if (charLevel) {
                if (selectedFeatures == null || selectedFeatures.contains(Feature.jaroWinkler)) {
                    featureNames.add(prefix + "jaroWinkler");
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.levenshtein)) {
                    featureNames.add(prefix + "levenshtein");
                }
            }

            if (!charLevel) {
                if (selectedFeatures == null || selectedFeatures.contains(Feature.mongeElkanJaroWinkler)) {
                    featureNames.add(prefix + "mongeElkanJaroWinkler");
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.mongeElkanLevenshtein)) {
                    featureNames.add(prefix + "mongeElkanLevenshtein");
                }
            }
        }

        private void buildFeatures(Language lang, String prefix,
                DoubleArrayList featureValues, String _label1, String _label2,
                String[] l1, String[] l2,
                boolean charLevel) {
            final String label1 = _label1 == null ? "" : _label1;
            final String label2 = _label2 == null ? "" : _label2;
            if (selectedFeatures == null || selectedFeatures.contains(Feature.lcs)) {
                featureValues.add(longestCommonSubsequence(l1, l2));
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.lc_suffix)) {
                featureValues.add(longestCommonSuffix(l1, l2));
            }
            if (selectedFeatures == null || selectedFeatures.contains(Feature.lc_prefix)) {
                featureValues.add(longestCommonPrefix(l1, l2));
            }
            if (!charLevel) {
                if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_1)) {
                    featureValues.add(ngramOverlap(l1, l2, 1, charLevel ? ngramWeighting : wordWeighting));
                }
            }

            if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_2)) {
                featureValues.add(ngramOverlap(l1, l2, 2, charLevel ? ngramWeighting : wordWeighting));
            }

            if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_3)) {
                featureValues.add(ngramOverlap(l1, l2, 3, charLevel ? ngramWeighting : wordWeighting));
            }

            if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_4)) {
                featureValues.add(ngramOverlap(l1, l2, 4, charLevel ? ngramWeighting : wordWeighting));
            }
            if (charLevel) {
                if (selectedFeatures == null || selectedFeatures.contains(Feature.ngram_5)) {
                    featureValues.add(ngramOverlap(l1, l2, 5, charLevel ? ngramWeighting : wordWeighting));
                }
            }
            final JaccardDice jd = jaccardDice(l1, l2);

            if (selectedFeatures == null || selectedFeatures.contains(Feature.jaccard)) {
                featureValues.add(jd.jaccard);
            }

            if (selectedFeatures == null || selectedFeatures.contains(Feature.dice)) {
                featureValues.add(jd.dice);
            }

            if (selectedFeatures == null || selectedFeatures.contains(Feature.containment)) {
                featureValues.add(jd.containment);
            }

            if (!charLevel) {

                if (selectedFeatures == null || selectedFeatures.contains(Feature.senLenRatio)) {
                    featureValues.add(sentenceLengthRatio(label1, label2));
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.aveWordLenRatio)) {
                    featureValues.add(aveWordLenRatio(l1, l2));
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.negation)) {
                    featureValues.add(shareNegation(lang, l1, l2) ? 1.0 : 0.0);
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.number)) {
                    featureValues.add(numberAgree(l1, l2));
                }
            }

            if (charLevel) {
                if (selectedFeatures == null || selectedFeatures.contains(Feature.jaroWinkler)) {
                    featureValues.add(JARO_WINKLER.apply(label1, label2));
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.levenshtein)) {
                    if(label1.length() > 0 && label2.length() > 0) {
                        featureValues.add(((double) LEVENSHTEIN.apply(label1, label2) * 2.0) / (label1.length() + label2.length()));
                    } else if(label1.length() > 0 || label2.length() > 0) {
                        featureValues.add(0.0);
                    } else {
                        featureValues.add(1.0);
                    }
                }
            }
            if (!charLevel) {
                if (selectedFeatures == null || selectedFeatures.contains(Feature.mongeElkanJaroWinkler)) {
                    featureValues.add(mongeElkan(l1, l2, (s, t) -> JARO_WINKLER.apply(s, t)));
                }
                if (selectedFeatures == null || selectedFeatures.contains(Feature.mongeElkanLevenshtein)) {
                    featureValues.add(mongeElkan(l1, l2, (s, t) -> {
                        if(s.length() > 0 && t.length() > 0) {
                            return 1.0 - (double) LEVENSHTEIN.apply(s, t) * 2.0 / (s.length() + t.length());
                        } else if(s.length() > 0 || t.length() > 0) {
                            return 0.0;
                        } else {
                            return 1.0;
                        }
                    }));
                }

            }
//        featureNames.add(prefix + "gst");
//        if(charLevel) {
//            String cl1 = label1.replaceAll("", " ").trim();
//            String cl2 = label2.replaceAll("", " ").trim();
//            featureValues.add(GreedyStringTiling.similarity(cl1, cl2));
//        } else {
//            featureValues.add(GreedyStringTiling.similarity(label1, label2));
//        }
        }

        @Override
        public org.insightcentre.uld.naisc.Feature[] extractFeatures(LensResult sp, NaiscListener log) {
            DoubleArrayList featureValues = new DoubleArrayList();
            final String label1 = lowerCase ? sp.string1.toLowerCase() : sp.string1;
            final String label2 = lowerCase ? sp.string2.toLowerCase() : sp.string2;
            final String[] l1tok = PrettyGoodTokenizer.tokenize(label1),
                    l2tok = PrettyGoodTokenizer.tokenize(label2);

            buildFeatures(sp.lang1, "", featureValues, label1, label2, l1tok, l2tok, false);
            if (labelCharFeatures) {
                buildFeatures(sp.lang2, "char-", featureValues, label1, label2, label1.split(""), label2.split(""), true);
            }

            return org.insightcentre.uld.naisc.Feature.mkArray(featureValues.toDoubleArray(), getFeatureNames());
        }

        public static double longestCommonSubsequence(String[] s1, String[] s2) {
            if (s1.length == 0 || s2.length == 0) {
                return 0;
            }
            int lcs = 0;
            for (int i = 0; i < s1.length; i++) {
                for (int j = 0; j < s2.length; j++) {
                    if (s1[i].equals(s2[j])) {
                        int lcs2;
                        for (lcs2 = 1; i + lcs2 < s1.length
                                && j + lcs2 < s2.length
                                && s1[i + lcs2].equals(s2[j + lcs2]); lcs2++) {
                        }
                        if (lcs2 > lcs) {
                            lcs = lcs2;
                        }
                    }
                }
            }
            return (double) lcs / (double) Math.max(s1.length, s2.length);
        }

        public static double longestCommonSuffix(String[] s1, String[] s2) {
            for(int i = 1; i <= min(s1.length, s2.length); i++) {
                if(!s1[s1.length - i].equals(s2[s2.length - i])) {
                    return 2.0 * (i-1) / (s1.length + s2.length);
                }
            }
            return 2.0 * min(s1.length, s2.length) /  (s1.length + s2.length);
        }

        public static double longestCommonPrefix(String[] s1, String[] s2) {
            for(int i = 0; i < min(s1.length, s2.length); i++) {
                if(!s1[i].equals(s2[i])) {
                    return 2.0 * i / (s1.length + s2.length);
                }
            }
            return 2.0 * min(s1.length, s2.length) /  (s1.length + s2.length);
        }

        private double mongeElkan(String[] l1, String[] l2, BiFunction<String, String, Double> function) {
            double sum = 0.0;
            for (String s : l1) {
                double max = Double.NEGATIVE_INFINITY;
                for (String t : l2) {
                    max = Math.max(function.apply(s, t), max);
                }
                sum += max;
            }
            if(l1.length > 0) {
                return sum / l1.length;
            } else {
                return 0.0;
            }
        }

        public static interface NGramWeighting {

            public double weight(String[] s, int i, int n);
        }

        public static class ConstantWeighting implements NGramWeighting {

            @Override
            public double weight(String[] s, int i, int n) {
                return 1.0;
            }
        }

        public static class SumWeighting implements NGramWeighting {

            private final Object2DoubleMap<String> wordWeighting;

            public SumWeighting(Object2DoubleMap<String> wordWeighting) {
                assert (wordWeighting != null);
                this.wordWeighting = wordWeighting;
            }

            @Override
            public double weight(String[] s, int i, int n) {
                double d = 0.0;
                for (int k = 0; k < n; k++) {
                    d += 1.0 - wordWeighting.getDouble(s[i + k]);
                }
                return d / n;
            }
        }

        public static class CatWeighting implements NGramWeighting {

            private final Object2DoubleMap<String> wordWeighting;

            public CatWeighting(Object2DoubleMap<String> wordWeighting) {
                this.wordWeighting = wordWeighting;
            }

            @Override
            public double weight(String[] s, int i, int n) {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < n; k++) {
                    sb.append(s[k]);
                }
                return (1.0 - wordWeighting.getDouble(sb.toString())) / n;
            }
        }

        public static double ngramOverlap(String[] s1, String[] s2, int n, NGramWeighting weighting) {
            if (s1.length < n || s2.length < n) {
                for (int i = 0; i < s1.length && i < s2.length; i++) {
                    if (!s1[i].equals(s2[i])) {
                        return 0;
                    }
                }
                return 1;
            }
            double ngramOverlap = 0;
            if (s2.length > 100) {
                HashMap<String, IntList> c2oMap = new HashMap<>();
                for(int j = 0; j + n <= s2.length; j++) {
                    if(!c2oMap.containsKey(s2[j])) {
                        c2oMap.put(s2[j], new IntArrayList());
                    }
                    c2oMap.get(s2[j]).add(j);
                }
                 for (int i = 0; i + n <= s1.length; i++) {
                     if(!c2oMap.containsKey(s1[i]))
                         continue;
                    
                    IntIterator ii = c2oMap.get(s1[i]).iterator();
                    OUTER:
                    while(ii.hasNext()) {
                        int j = ii.nextInt();
                        for (int k = 1; k < n; k++) {
                            if (!s1[i + k].equals(s2[j + k])) {
                                continue OUTER;
                            }
                        }
                        ngramOverlap += weighting.weight(s1, i, n);
                        break;
                    }
                }        

            } else {
                for (int i = 0; i + n <= s1.length; i++) {
                    OUTER:
                    for (int j = 0; j + n <= s2.length; j++) {
                        for (int k = 0; k < n; k++) {
                            if (!s1[i + k].equals(s2[j + k])) {
                                continue OUTER;
                            }
                        }
                        ngramOverlap += weighting.weight(s1, i, n);
                        break;
                    }
                }
            }
            //return ((double) ngramOverlap) / (s1.length - n + 1);
            if(ngramOverlap != 0.0)
                return 2.0 / ( (double)s1.length /  ngramOverlap + (double)s2.length / ngramOverlap);
            else
                return 0.0;
        }

        public static final class JaccardDice {

            public final double jaccard;
            public final double dice;
            public final double containment;

            public JaccardDice(double jaccard, double dice, double containment) {
                this.jaccard = jaccard;
                this.dice = dice;
                this.containment = containment;
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 37 * hash + (int) (Double.doubleToLongBits(this.jaccard) ^ (Double.doubleToLongBits(this.jaccard) >>> 32));
                hash = 37 * hash + (int) (Double.doubleToLongBits(this.dice) ^ (Double.doubleToLongBits(this.dice) >>> 32));
                hash = 37 * hash + (int) (Double.doubleToLongBits(this.containment) ^ (Double.doubleToLongBits(this.containment) >>> 32));
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final JaccardDice other = (JaccardDice) obj;
                if (Double.doubleToLongBits(this.jaccard) != Double.doubleToLongBits(other.jaccard)) {
                    return false;
                }
                if (Double.doubleToLongBits(this.dice) != Double.doubleToLongBits(other.dice)) {
                    return false;
                }
                if (Double.doubleToLongBits(this.containment) != Double.doubleToLongBits(other.containment)) {
                    return false;
                }
                return true;
            }

            @Override
            public String toString() {
                return "JaccardDice{" + "jaccard=" + jaccard + ", dice=" + dice + ", containment=" + containment + '}';
            }
        }

        public static JaccardDice jaccardDice(String[] s1, String[] s2) {
            if (s1.length == 0 || s2.length == 0) {
                return new JaccardDice(0, 0, 0);
            }
            final HashSet<String> ss1 = new HashSet<>(Arrays.asList(s1));
            final HashSet<String> ss2 = new HashSet<>(Arrays.asList(s2));
            int a = ss1.size();
            int b = ss2.size();
            ss1.retainAll(ss2);
            int ab = ss1.size();

            return new JaccardDice(
                    ((double) ab) / (a + b - ab),
                    ((double) 2.0 * ab) / (a + b),
                    ((double) ab) / Math.min(a, b));
        }

        public static double symmetrizedRatio(double x, double y) {
            if (x > y) {
                return 1.0 - (y / x);
            } else if(y != 0.0) {
                return 1.0 - (x / y);
            } else if(x != 0.0) {
                return 1.0;
            } else {
                return 0.0;
            }
        }

        public static double sentenceLengthRatio(String s1, String s2) {
            if (s1.length() == 0 || s2.length() == 0) {
                return 0;
            }
            return symmetrizedRatio((double) s1.length(), (double) s2.length());
        }

        private static double aveWordLen(String[] s1) {
            int n = 0;
            int N = s1.length;
            for (int i = 0; i < s1.length; i++) {
                n += s1[i].length();
            }
            return ((double) n) / N;
        }

        public static double aveWordLenRatio(String[] s1, String[] s2) {
            if (s1.length == 0 || s2.length == 0) {
                return 0;
            }
            return symmetrizedRatio(aveWordLen(s1), aveWordLen(s2));
        }

        @Override
        public void close() throws IOException {
        }

        public static boolean shareNegation(Language lang, String[] s1, String[] s2) {
            if (!NegationWords.byLang.containsKey(lang)) {
                return false;
            }
            Set<String> negationWords = NegationWords.byLang.get(lang);
            boolean negated1 = false, negated2 = false;
            for (String s : s1) {
                negated1 = negated1 || negationWords.contains(s);
            }
            for (String s : s2) {
                negated2 = negated2 || negationWords.contains(s);
            }
            return negated1 == negated2;
        }

        private static List<Double> extractNumbers(String[] s1) {
            List<Double> nums = new ArrayList<>();

            for (String s : s1) {
                try {
                    nums.add(Double.parseDouble(s));
                } catch (NumberFormatException x) { // Ignore 
                }
            }

            return nums;
        }

        public static double numberAgree(String[] s1, String[] s2) {
            double agree = 0.0;
            List<Double> l1 = extractNumbers(s1);
            List<Double> l2 = extractNumbers(s2);
            if (l1.isEmpty() && l2.isEmpty()) {
                return 1.0;
            } else if (l1.isEmpty() || l2.isEmpty()) {
                return 0.0;
            }
            OUTER:
            for (double d : l1) {
                for (double d2 : l2) {
                    if (abs(d - d2) < 1) {
                        agree += 1;
                        continue OUTER;
                    }
                }
            }
            return agree / Math.max(l1.size(), l2.size());
        }

    }

    private static class NegationWords {

        public final static Map<Language, Set<String>> byLang = new HashMap<>();

        static {
            byLang.put(ENGLISH, new HashSet<>(Arrays.asList("not", "never", "neither", "nor", "nobody", "nothing", "don't", "won't", "can't", "doesn't", "aren't", "isn't", "haven't")));
        }
    }
}
