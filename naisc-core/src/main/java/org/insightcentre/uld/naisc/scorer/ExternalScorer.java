package org.insightcentre.uld.naisc.scorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExternalScorer implements ScorerFactory {
    @Override
    public String id() {
        return "external";
    }

    @Override
    public List<Scorer> makeScorer(Map<String, Object> params, File modelPath) throws IOException {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        return null;
    }

    @Override
    public Option<ScorerTrainer> makeTrainer(Map<String, Object> params, String property, File modelPath) {
        return new None<>();
    }

    public static class Configuration {
        @ConfigurationParameter(description = "Endpoint for scorer service, for example http://localhost:8080")
        public String endpoint = "http://localhost:8080";

        @ConfigurationParameter(description = "The configuration parameter for the external parameter")
        public String configName = "default";

        @ConfigurationParameter(description = "The path or null if the path is $endpoint/naisc/$configName/score")
        public String path;
    }

    private static class ExternalScorerImpl implements Scorer {
        private final String relation;
        private final String endpoint;

        public ExternalScorerImpl(String relation, String endpoint) {
            this.relation = relation;
            this.endpoint = endpoint;
        }

        @Override
        public ScoreResult similarity(FeatureSet features, NaiscListener log) {
            return null;
        }

        @Override
        public String relation() {
            return null;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
