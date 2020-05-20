package org.insightcentre.uld.naisc.graph;

import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.util.Lazy;

import java.util.Map;

public class ExternalGraphFeature implements GraphFeatureFactory {
    @Override
    public GraphFeature makeFeature(Dataset sparqlData, Map<String, Object> params, Lazy<Analysis> analysis, Lazy<AlignmentSet> prelinking, NaiscListener listener) {
        return null;
    }


    public static class Configuration {
        @ConfigurationParameter(description = "Endpoint for text feature service, for example http://localhost:8080")
        public String endpoint = "http://localhost:8080";

        @ConfigurationParameter(description = "The configuration parameter for the external parameter")
        public String configName = "default";

        @ConfigurationParameter(description = "The path or null if the path is $endpoint/naisc/$configName/text_features")
        public String path;

        @ConfigurationParameter(description = "The name of this feature")
        public String id = "external";
    }

    private static class ExternalGraphFeatureImpl implements GraphFeature {

        @Override
        public String id() {
            return null;
        }

        @Override
        public Feature[] extractFeatures(Resource entity1, Resource entity2, NaiscListener log) {
            return new Feature[0];
        }

        @Override
        public String[] getFeatureNames() {
            return new String[0];
        }
    }
}
