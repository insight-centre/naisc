package org.insightcentre.uld.naisc.scorer;

import org.apache.jena.vocabulary.SKOS;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.graph.ACDHFeature;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ACDHScorer  implements ScorerFactory {

    @ConfigurationClass("ACDH scorer can be used to pass the ACDH results through without any learning. " +
            "This only works if ACDH features are the only ones used, otherwise use a supervised system like LibSVM")
    private static class Configuration {

    }
    @Override
    public Scorer makeScorer(Map<String, Object> params, File modelPath) throws IOException {
        return new ACDHScorerImpl();
    }

    @Override
    public Option<ScorerTrainer> makeTrainer(Map<String, Object> params, String property, File modelPath) {
        return new None<>();
    }

    private static class ACDHScorerImpl implements Scorer {

        @Override
        public List<ScoreResult> similarity(FeatureSet features, NaiscListener log) throws ModelNotTrainedException {
            List<ScoreResult> result = new ArrayList<>();
            for(Feature f : features) {
                if(f.name.equals("exact-acdh")) {
                    result.add(new ScoreResult(f.value, SKOS.exactMatch.getURI()));
                } else if(f.name.equals("narrower-acdh")) {
                    result.add(new ScoreResult(f.value, SKOS.narrowMatch.getURI()));
                } else if(f.name.equals("broader-acdh")) {
                    result.add(new ScoreResult(f.value, SKOS.broadMatch.getURI()));
                } else if(f.name.equals("related-acdh")) {
                    result.add(new ScoreResult(f.value, SKOS.relatedMatch.getURI()));
                } else if(!f.name.equals("none-acdh")) {
                    log.message(NaiscListener.Stage.SCORING, NaiscListener.Level.WARNING, "Unexpected extra feature: " + f.name);
                }
            }
            return result;
        }

        @Override
        public void close() throws IOException {

        }
    }
 }
