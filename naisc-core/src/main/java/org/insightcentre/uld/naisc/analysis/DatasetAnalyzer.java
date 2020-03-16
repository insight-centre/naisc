package org.insightcentre.uld.naisc.analysis;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.util.URI2Label;

/**
 * Analyse the labels so that sensible labels can be extracted
 *
 * @author John McCrae
 */
public class DatasetAnalyzer {

    public int overlapSize(Set<String> left, List<String> right) {
        int overlap = 0;
        for (String s : right) {
            if (left.contains(s)) {
                overlap++;
            }
        }
        return overlap;
    }

    public List<MatchResult> analyseMatch(Map<String, List<String>> left, Map<String, List<String>> right) {
        List<MatchResult> results = new ArrayList<>();
        for (String leftUri : left.keySet()) {
            Set<String> leftSet = new HashSet<>(left.get(leftUri));
            for (String rightUri : right.keySet()) {
                results.add(new MatchResult(leftUri, rightUri, left.get(leftUri).size(), right.get(rightUri).size(), overlapSize(leftSet, right.get(rightUri))));
            }
        }
        return results;
    }

    public Analysis analyseModel(Dataset leftModel, Dataset rightModel) {
        Map<String, List<String>> leftAnalysis = new HashMap<>();
        Map<String, Set<String>> leftPropBySubj = new HashMap<>();
        Set<String> leftSubjects = new HashSet<>();
        Set<String> leftDataProps = new HashSet<>();
        Set<String> leftClasses = new HashSet<>();
        List<Set<Resource>> leftComponents = analyzeLabels(leftModel, leftSubjects, leftAnalysis, leftPropBySubj, leftDataProps, leftClasses);
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
                        //-1.0,
                        naturalLangLike(e.getValue()),
                        uniqueness(e.getValue()),
                        diversity(e.getValue()), false));
            }
        }

        Map<String, List<String>> rightAnalysis = new HashMap<>();
        Map<String, Set<String>> rightPropBySubj = new HashMap<>();
        Set<String> rightSubjects = new HashSet<>();
        Set<String> rightDataProps = new HashSet<>();
        Set<String> rightClasses = new HashSet<>();
        List<Set<Resource>> rightComponents = analyzeLabels(rightModel, rightSubjects, rightAnalysis, rightPropBySubj, rightDataProps, rightClasses);
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
                        //-1.0,
                        naturalLangLike(e.getValue()),
                        uniqueness(e.getValue()),
                        diversity(e.getValue()), false));
            }

        }

        return new Analysis(leftResult, rightResult, analyseMatch(leftAnalysis, rightAnalysis),
                leftClasses, rightClasses, lcc(leftComponents), lcc(rightComponents),
                leftSubjects.size(), rightSubjects.size());
    }

    private static class Components {
        // For efficiency we maintain the component list simultaneously in two
        // similar data structures
        List<Set<Resource>> components = new ArrayList<>();
        Object2IntMap ids = new Object2IntOpenHashMap<>();

        public int componentNumber(Resource r) {
            return ids.getInt(r);
        }

        public int size() {
            return components.size();
        }

        public void add(Resource r) {
            ids.put(r, components.size());
            components.add(new HashSet<>(Arrays.asList(r)));
        }

        public void add(Resource r, Resource r2) {
            ids.put(r, components.size());
            ids.put(r2, components.size());
            components.add(new HashSet<>(Arrays.asList(r, r2)));
        }

        public void add(int j, Resource r) {
            ids.put(r, j);
            components.get(j).add(r);
        }

        public void merge(int i, int j) {

            Set<Resource> ci = components.get(i);
            Set<Resource> cj = components.get(j);
            if (ci.size() < cj.size()) {
                for(Resource r : ci) {
                    ids.put(r, j);
                }
                cj.addAll(ci);
                components.remove(i);
            } else {
                for(Resource r : cj) {
                    ids.put(r, i);
                }
                ci.addAll(cj);
                components.remove(j);
            }
        }
        
        public List<Set<Resource>> asList() { return components; }
    }


    private List<Set<Resource>> analyzeLabels(Dataset model, Set<String> subjects,
            Map<String, List<String>> analysis, Map<String, Set<String>> propBySubj,
            Set<String> dataProps, Set<String> classes) {
        Components components = new Components();
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
                    Literal l = stmt.getObject().asLiteral();
                    if(l.getDatatypeURI() != null && l.getDatatypeURI().startsWith("http://www.w3.org/2001/XMLSchema#") &&
                            !l.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string"))
                        continue; // Skip all non-string data
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
            if (stmt.getObject().isResource()) {
                int i = components.componentNumber(stmt.getSubject());
                int j = components.componentNumber(stmt.getObject().asResource());
                if (i == components.size()) {
                    if (j == components.size()) {
                        components.add(stmt.getSubject(), stmt.getObject().asResource());
                        //components.add(new HashSet<>(Arrays.asList(stmt.getSubject(), stmt.getObject().asResource())));
                    } else {
                        components.add(stmt.getSubject());
                        components.add(j, stmt.getObject().asResource());
                    }
                } else {
                    if (j == components.size()) {
                        components.add(stmt.getObject().asResource());
                        components.add(i, stmt.getSubject());
                    } else {
                        if (i != j) {
                            components.merge(i, j);
                        } // No else case here, they are already connected!
                    }
                }
            }
        }
        List<String> fromURIs = subjects.stream().map(x -> URI2Label.fromURI(x)).collect(Collectors.toList());
        analysis.put("", fromURIs);
        propBySubj.put("", subjects);
        return components.asList();
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

    private static double lcc(List<Set<Resource>> components) {
        int total = 0;
        int max = -1;
        for (Set<Resource> component : components) {
            if (component.size() > max) {
                max = component.size();
            }
            total += component.size();
        }
        if (max >= 0) {
            return (double) max / total;
        } else {
            return 0.0;
        }
    }
}
