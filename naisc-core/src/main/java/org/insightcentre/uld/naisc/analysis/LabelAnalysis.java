package org.insightcentre.uld.naisc.analysis;

import java.io.UnsupportedEncodingException;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

/**
 * Analyse the labels so that sensible labels can be extracted
 *
 * @author John McCrae
 */
public class LabelAnalysis {

    public static class LabelResult {

        public final String uri;
        public final int total;
        public final double coverage;
        public final double naturalLangLike;
        public final double uniqueness;
        public final double diversity;

        public LabelResult(String uri, int total, double coverage, double naturalLangLike, double uniqueness, double diversity) {
            this.uri = uri;
            this.total = total;
            this.coverage = coverage;
            this.naturalLangLike = naturalLangLike;
            this.uniqueness = uniqueness;
            this.diversity = diversity;
        }

    }

    public List<LabelResult> analyse(Model model) {
        Map<String, List<String>> analysis = new HashMap<>();
        Map<String, Set<String>> propBySubj = new HashMap<>();
        Set<String> subjects = new HashSet<>();
        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.next();
            if (stmt.getSubject().isURIResource()) {
                subjects.add(stmt.getSubject().getURI());
                if (stmt.getObject().isLiteral()) {
                    String prop = stmt.getPredicate().getURI();
                    if (!analysis.containsKey(prop)) {
                        analysis.put(prop, new ArrayList<>());
                        propBySubj.put(prop, new HashSet<>());
                    }
                    analysis.get(prop).add(stmt.getObject().asLiteral().getString());
                    propBySubj.get(prop).add(stmt.getSubject().getURI());
                }
            }
        }
        List<LabelResult> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : analysis.entrySet()) {
            result.add(new LabelResult(e.getKey(), propBySubj.get(e.getKey()).size(),
                    (double) propBySubj.get(e.getKey()).size() / subjects.size(),
                    naturalLangLike(e.getValue()),
                    uniqueness(e.getValue()),
                    diversity(e.getValue())));

        }
        List<String> fromURIs = subjects.stream().map(x -> fromURI(x)).collect(Collectors.toList());
        result.add(new LabelResult("", subjects.size(),
                1.0, naturalLangLike(fromURIs),
                uniqueness(fromURIs),
                diversity(fromURIs)));
        return result;
    }

    private double naturalLangLike(List<String> strings) {
        int n = 0;
        for (String s : strings) {
            if (isNaturalLangLike(s)) {
                n++;
            }
        }
        return (double) n / strings.size();
    }

    private final double IDEAL_LETTER_RATIO = 0.77;
    private final double IDEAL_SPACE_RATIO = 0.18;
    private final double MIN_RATIO = 0.1;

    public boolean isNaturalLangLike(String s) {
        int letters = 0;
        int spaces = 0;
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) {
                letters += 1;
            } else if (Character.isWhitespace(c)) {
                spaces += 1;
            }
        }
        double letterRatio = new BetaDistribution(letters + 1, s.length() - letters + 1).density(IDEAL_LETTER_RATIO);
        double spaceRatio = new BetaDistribution(spaces + 1, s.length() - spaces + 1).density(IDEAL_SPACE_RATIO);
        return letterRatio > MIN_RATIO && spaceRatio > MIN_RATIO;
    }

    public double diversity(List<String> strings) {
        if (strings.isEmpty()) {
            return 0.0;
        }
        Set<String> withoutDupes = new HashSet<>(strings);
        int n = withoutDupes.size();
        int N = strings.size();
        if(N <= 1)
            return 1.0;
        // Not sure this really makes sense, but it returns a value between 0 and 1
        // which is penalized for very high diversity (all values unique) or 
        // very low diversity (all values the same)
        double alpha = 1.0 / 2.0 / (sqrt(N) - 1);
        return sqrt(n - 1) * exp(-alpha * n) / sqrt(sqrt(N) - 1) / exp(-alpha * sqrt(N));
    }

    public double uniqueness(List<String> strings) {
        if (strings.isEmpty()) {
            return 0.0;
        }
        Set<String> withoutDupes = new HashSet<>(strings);
        int n = withoutDupes.size();
        int N = strings.size();
        return (double) n / N;
    }

    private String fromURI(String s) {
        try {
            URI uri = URI.create(s);
            final String raw;
            if (uri.getFragment() != null) {
                raw = uri.getFragment();
            } else {

                String path = uri.getPath();
                if (path.lastIndexOf("/") >= 0) {
                    raw = path.substring(path.lastIndexOf("/") + 1);
                } else if (path.lastIndexOf("\\") >= 0) {
                    raw = path.substring(path.lastIndexOf("\\") + 1);
                } else {
                    raw = path;
                }
            }
            final String clean;
            try {
                clean = URLDecoder.decode(org.insightcentre.uld.naisc.lens.URI.deCamelCase(raw).replace("_", " "), "UTF-8");
            } catch (UnsupportedEncodingException x) {
                return "";
            }
            return clean;
        } catch (Exception x) {
            return "";
        }
    }
}
