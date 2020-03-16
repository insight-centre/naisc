package org.insightcentre.uld.naisc.util;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A pretty good tokenizer
 *
 * @author John McCrae
 */
public final class PrettyGoodTokenizer {

    private final static Pattern pattern1 = Pattern.compile("(\\.\\.\\.+|[\\p{Po}\\p{Ps}\\p{Pe}\\p{Pi}\\p{Pf}\u2013\u2014\u2015&&[^'\\.]]|(?<!(\\.|\\.\\p{L}))\\.(?=[\\p{Z}\\p{Pf}\\p{Pe}]|\\Z)|(?<!\\p{L})'(?!\\p{L}))");
    private final static Pattern pattern2 = Pattern.compile("\\p{C}|^\\p{Z}+|\\p{Z}+$");

    private final static SimpleCache<String, String[]> cache = new SimpleCache(10000);

    private final static class DoTokenize implements Function<String, String[]> {

        @Override
        public String[] apply(String s) {
            String s1 = pattern1.matcher(s).replaceAll(" $1 ");
            String s2 = pattern2.matcher(s1).replaceAll("");
            return s2.split("\\p{Z}+");
        }

    }
    private final static DoTokenize doTokenize = new DoTokenize();

    public static String[] tokenize(String s) {
        return cache.get(s, doTokenize);
    }
}
