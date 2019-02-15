package org.insightcentre.uld.naisc.util;

import eu.monnetproject.lang.Language;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Literal;

/**
 * Tools for working with labels
 *
 * @author John McCrae <john@mccr.ae>
 */
public final class Labels {

    /**
     * Join a string. e.g., elem[0] + separator + elem[1] + separator + elem[2]
     *
     * @param separator The separator
     * @param elems The elements to join
     * @return The string join
     */
    public static String strJoin(String separator, String[] elems) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elems.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(elems[i]);
        }
        return sb.toString();
    }

    /**
     * Join a string. e.g., elem[0] + separator + elem[1] + separator + elem[2]
     *
     * @param separator The separator
     * @param elems The elements to join
     * @return The string join
     */
    public static String strJoin(String separator, Collection<String> elems) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String s : elems) {
            if (i++ > 0) {
                sb.append(separator);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Join a string. e.g., elem[from] + separator + ... + separator +
     * elem[to-1]
     *
     * @param separator The separator
     * @param elems The elements to join
     * @param from The element to start joinging from
     * @param to The last element to join
     * @return The string join
     */
    public static String strJoin(String separator, String[] elems, int from, int to) {
        assert (to <= elems.length);
        assert (from >= 0);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(separator);
            }
            sb.append(elems[i]);
        }
        return sb.toString();
    }

    private Labels() {
    }

    private static int minDistance(String word1, String word2) {
        int len1 = word1.length();
        int len2 = word2.length();

        // len1+1, len2+1, because finally return dp[len1][len2]
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        //iterate though, and check last char
        for (int i = 0; i < len1; i++) {
            char c1 = word1.charAt(i);
            for (int j = 0; j < len2; j++) {
                char c2 = word2.charAt(j);

                //if last two chars equal
                if (c1 == c2) {
                    //update dp value for +1 length
                    dp[i + 1][j + 1] = dp[i][j];
                } else {
                    int replace = dp[i][j] + 1;
                    int insert = dp[i][j + 1] + 1;
                    int delete = dp[i + 1][j] + 1;

                    int min = replace > insert ? insert : replace;
                    min = delete > min ? min : delete;
                    dp[i + 1][j + 1] = min;
                }
            }
        }

        return dp[len1][len2];
    }

    /**
     * Find the two closest labels from a set of labels, where distance is
     * Levenshtein.
     *
     * @param ss1 The first set of candidate labels
     * @param ss2 The second set of candidate labels
     * @return A pair such that _1 is in ss1 and _2 is in ss2 and dist(_1,_2) is
     * minimal for all values for ss1 x ss2
     */
    public static StringPair closestLabels(List<String> ss1, List<String> ss2) {
        if (ss1.isEmpty() || ss2.isEmpty()) {
            return new StringPair("", "");
        }
        if (ss1.size() == 1 && ss2.size() == 1) {
            return new StringPair(ss1.get(0), ss2.get(0));
        }
        StringPair min = null;
        int minDistance = Integer.MAX_VALUE;
        for (String s1 : ss1) {
            for (String s2 : ss2) {
                int dist = minDistance(s1, s2);
                if (dist < minDistance) {
                    min = new StringPair(s1, s2);
                    minDistance = dist;
                }
            }
        }
        if (min == null) {
            throw new IllegalArgumentException("Empty list of labels!");
        }
        return min;
    }

    /**
     * Find the two closest labels from a set of labels by language, where
     * distance is Levenshtein.
     *
     * @param ss1 The first set of candidate labels
     * @param ss2 The second set of candidate labels
     * @return A pair such that _1 is in ss1 and _2 is in ss2 and dist(_1,_2) is
     * minimal for all values for ss1 x ss2
     */
    public static List<LangStringPair> closestLabelsByLang(List<Literal> ss1, List<Literal> ss2) {
        final Map<Language, Pair<List<String>, List<String>>> map = new HashMap<>();
        for (Literal l : ss1) {
            final Language lang;
            if (l.getLanguage() != null && !l.getLanguage().equals("")) {
                lang = Language.get(l.getLanguage());
                if (!map.containsKey(lang)) {
                    map.put(lang, new Pair<>(new ArrayList<>(), new ArrayList<>()));
                }
                map.get(lang)._1.add(l.getString());
            }
        }

        for (Literal l : ss2) {
            final Language lang;
            if (l.getLanguage() != null && !l.getLanguage().equals("")) {
                lang = Language.get(l.getLanguage());
                if (!map.containsKey(lang)) {
                    map.put(lang, new Pair<>(new ArrayList<>(), new ArrayList<>()));
                }
                map.get(lang)._2.add(l.getString());
            }
        }

        for (Literal l : ss1) {
            if (l.getLanguage() == null || l.getLanguage().equals("")) {
                if (map.isEmpty()) {
                    if (!map.containsKey(Language.UNDEFINED)) {
                        map.put(Language.UNDEFINED, new Pair<>(new ArrayList<>(), new ArrayList<>()));
                    }
                    map.get(Language.UNDEFINED)._1.add(l.getString());

                } else {
                    for (Language lang : map.keySet()) {
                        map.get(lang)._1.add(l.getString());
                    }
                }
            }
        }

        for (Literal l : ss2) {
            if (l.getLanguage() == null || l.getLanguage().equals("")) {
                if (map.isEmpty()) {
                    if (!map.containsKey(Language.UNDEFINED)) {
                        map.put(Language.UNDEFINED, new Pair<>(new ArrayList<>(), new ArrayList<>()));
                    }
                    map.get(Language.UNDEFINED)._2.add(l.getString());

                } else {
                    for (Language lang : map.keySet()) {
                        map.get(lang)._2.add(l.getString());
                    }
                }
            }
        }
        List<LangStringPair> pairs = new ArrayList<>();
        for (Map.Entry<Language, Pair<List<String>, List<String>>> e : map.entrySet()) {
            if (!e.getValue()._1.isEmpty() && !e.getValue()._2.isEmpty()) {
                StringPair sp = closestLabels(e.getValue()._1, e.getValue()._2);
                pairs.add(new LangStringPair(e.getKey(), e.getKey(), sp._1, sp._2));
            }
        }
        return pairs;
    }

}
