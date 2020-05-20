package org.insightcentre.uld.naisc.lens;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.NaiscListener.Stage;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.LabelResult;
import org.insightcentre.uld.naisc.analysis.MatchResult;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Tool for automatically creating a configuration of lenses from an analysis
 *
 * @author John McCrae
 */
public class LensAutoConfig {

    public List<Lens> autoConfiguration(Analysis analysis, Dataset leftModel, NaiscListener log) {
        List<Lens> lenses = new ArrayList<>();
        log.message(Stage.INITIALIZING, NaiscListener.Level.INFO, "Automatically configuring lenses");

        // 1. Scan for any good labels in the left dataset
        Set<String> leftCandidates = new HashSet<>();
        for (LabelResult prop : analysis.leftLabels) {
            if (prop.isLabelLens()) {
                leftCandidates.add(prop.uri);
            }
        }

        // 2. If the same label property is used in both datasets then add it as a label
        Set<String> rightCandidates = new HashSet<>();
        for (LabelResult prop : analysis.rightLabels) {
            if (prop.isLabelLens()) {
                if (prop.uri.equals("") && leftCandidates.contains("")) {
                    lenses.add(new URI.URIImpl(URI.LabelLocation.infer, URI.LabelForm.smart, "_"));
                    leftCandidates.remove("");
                    log.message(Stage.INITIALIZING, NaiscListener.Level.INFO, String.format("Using URIs as a lens (%.4f)", prop.naturalLangLike));
                } else if (leftCandidates.contains(prop.uri)) {
                    lenses.add(new Label.LabelImpl(prop.uri, prop.uri, prop.uri, leftModel, null));
                    leftCandidates.remove(prop.uri);
                    log.message(Stage.INITIALIZING, NaiscListener.Level.INFO, String.format("Using %s as a lens (%.4f)", prop.uri, prop.naturalLangLike));
                } else {
                    rightCandidates.add(prop.uri);
                }
            }
        }

        // 3. If there are any good properties that are not in both datasets then try to find
        // The best matching in a greedy fashion
        if (!leftCandidates.isEmpty() && !rightCandidates.isEmpty()) {
            Object2DoubleMap<Pair<String, String>> scores = new Object2DoubleOpenHashMap<>();
            for (MatchResult mr : analysis.matching) {
                scores.put(new Pair<>(mr.leftUri, mr.rightUri), 2.0 * (double) mr.coverage / (mr.leftTotal + mr.rightTotal));
            }
            List<Pair<String, String>> keys = new ArrayList<>(scores.keySet());
            keys.sort((x, y) -> {
                int d = -Double.compare(scores.getDouble(x), scores.getDouble(y));
                if (d == 0) {
                    return Integer.compare(x.hashCode(), y.hashCode());
                } else {
                    return d;
                }
            });
            for(Pair<String,String> elem : keys) {
                if(scores.getDouble(elem) < 0.1) 
                    break;
                if(leftCandidates.contains(elem._1) && rightCandidates.contains(elem._2)) {
                    lenses.add(new Label.LabelImpl(elem._1, elem._2, elem._1, leftModel, null));
                    log.message(Stage.INITIALIZING, NaiscListener.Level.INFO, "Using " +  elem._1 +" with " + elem._2 + " as a lens");
                    leftCandidates.remove(elem._1);
                    rightCandidates.remove(elem._2);
                }
            }
        }

        return lenses;
    }
}
