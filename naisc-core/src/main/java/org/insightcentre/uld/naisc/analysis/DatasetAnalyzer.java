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
import org.insightcentre.uld.naisc.util.URI2Label;

/**
 * Analyse the labels so that sensible labels can be extracted
 *
 * @author John McCrae
 */
public class DatasetAnalyzer {

    public List<MatchResult> analyseMatch(Map<String, List<String>> left, Map<String, List<String>> right) {
        List<MatchResult> results = new ArrayList<>();
        for (String leftUri : left.keySet()) {
            for (String rightUri : right.keySet()) {
                Set<String> overlap = new HashSet<>(left.get(leftUri));
                overlap.retainAll(right.get(rightUri));
                results.add(new MatchResult(leftUri, rightUri, left.get(leftUri).size(), right.get(rightUri).size(), overlap.size()));
            }
        }
        return results;
    }

    public Analysis analyseModel(Model leftModel, Model rightModel) {
        Map<String, List<String>> leftAnalysis = new HashMap<>();
        Map<String, Set<String>> leftPropBySubj = new HashMap<>();
        Set<String> leftSubjects = new HashSet<>();
        Set<String> leftDataProps = new HashSet<>();
        Set<String> leftClasses = new HashSet<>();
        analyzeLabels(leftModel, leftSubjects, leftAnalysis, leftPropBySubj, leftDataProps, leftClasses);
        List<LabelResult> leftResult = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : leftAnalysis.entrySet()) {
            if (leftDataProps.contains(e.getKey())) {
                leftResult.add(new LabelResult(e.getKey(), leftPropBySubj.get(e.getKey()).size(),
                        (double) leftPropBySubj.get(e.getKey()).size() / leftSubjects.size(),
                        naturalLangLike(e.getValue()),
                        uniqueness(e.getValue()),
                        diversity(e.getValue()), true));
            } else {

                leftResult.add(new LabelResult(e.getKey(), leftPropBySubj.get(e.getKey()).size(),
                        (double) leftPropBySubj.get(e.getKey()).size() / leftSubjects.size(),
                        -1.0,
                        uniqueness(e.getValue()),
                        diversity(e.getValue()), false));
            }
        }

        Map<String, List<String>> rightAnalysis = new HashMap<>();
        Map<String, Set<String>> rightPropBySubj = new HashMap<>();
        Set<String> rightSubjects = new HashSet<>();
        Set<String> rightDataProps = new HashSet<>();
        Set<String> rightClasses = new HashSet<>();
        analyzeLabels(rightModel, rightSubjects, rightAnalysis, rightPropBySubj, rightDataProps, rightClasses);
        List<LabelResult> rightResult = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : rightAnalysis.entrySet()) {
            if (rightDataProps.contains(e.getKey())) {
                rightResult.add(new LabelResult(e.getKey(), rightPropBySubj.get(e.getKey()).size(),
                        (double) rightPropBySubj.get(e.getKey()).size() / rightSubjects.size(),
                        naturalLangLike(e.getValue()),
                        uniqueness(e.getValue()),
                        diversity(e.getValue()), true));
            } else {

                rightResult.add(new LabelResult(e.getKey(), rightPropBySubj.get(e.getKey()).size(),
                        (double) rightPropBySubj.get(e.getKey()).size() / rightSubjects.size(),
                        -1.0,
                        uniqueness(e.getValue()),
                        diversity(e.getValue()), false));
            }

        }

        return new Analysis(leftResult, rightResult, analyseMatch(leftAnalysis, rightAnalysis),
                leftClasses, rightClasses);
    }

    private void analyzeLabels(Model model, Set<String> subjects,
            Map<String, List<String>> analysis, Map<String, Set<String>> propBySubj,
            Set<String> dataProps, Set<String> classes) {
        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.next();
            if (stmt.getSubject().isURIResource()) {
                subjects.add(stmt.getSubject().getURI());
                if (stmt.getObject().isLiteral()) {
                    String prop = stmt.getPredicate().getURI();
                    if (!analysis.containsKey(prop) || analysis.containsKey(prop) && !dataProps.contains(prop)) {
                        analysis.put(prop, new ArrayList<>());
                        propBySubj.put(prop, new HashSet<>());
                    }
                    analysis.get(prop).add(stmt.getObject().asLiteral().getString());
                    propBySubj.get(prop).add(stmt.getSubject().getURI());
                    dataProps.add(prop);
                } else if (stmt.getObject().isURIResource()) {
                    String prop = stmt.getPredicate().getURI();
                    if (prop.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                        classes.add(stmt.getObject().asResource().getURI());
                    } else if (!dataProps.contains(prop)) {
                        if (!analysis.containsKey(prop)) {
                            analysis.put(prop, new ArrayList<>());
                            propBySubj.put(prop, new HashSet<>());
                        }
                        analysis.get(prop).add(stmt.getObject().asResource().getURI());
                        propBySubj.put(prop, new HashSet<>());
                    }
                }
            }
        }
        List<String> fromURIs = subjects.stream().map(x -> URI2Label.fromURI(x)).collect(Collectors.toList());
        analysis.put("", fromURIs);
        propBySubj.put("", subjects);
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
        if (N <= 1) {
            return 1.0;
        }
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

}
