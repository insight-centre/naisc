package org.insightcentre.uld.naisc.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.util.FastPPR;
import org.insightcentre.uld.naisc.util.FastPPR.DirectedGraph;
import org.insightcentre.uld.naisc.util.FastPPR.FastPPRConfiguration;
import org.insightcentre.uld.naisc.util.Lazy;

/**
 * Implement a PPR style model
 *
 * @author John McCrae
 */
public class PPR implements GraphFeatureFactory {

    @Override
    public GraphFeature makeFeature(Dataset sparqlData, Map<String, Object> params, Lazy<Analysis> analysis, AlignmentSet prelinking, NaiscListener listener) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        Object2IntMap<Resource> identifiers = new Object2IntOpenHashMap<>();
        DirectedGraph graph = buildGraph(sparqlData, prelinking, identifiers);

        FastPPRConfiguration pprConfig = new FastPPRConfiguration(config.pprSignificanceThreshold, config.reversePPRApproximationFactor, config.teleportProbability, config.forwardStepsPerReverseStep, config.nWalksConstant);

        return new PPRImpl(graph, identifiers, pprConfig, sparqlData);
    }

    public static class Configuration {

        float pprSignificanceThreshold = 1.0e-3f;
        float reversePPRApproximationFactor = 1.0f / 6.0f;
        float teleportProbability = 0.2f;
        float forwardStepsPerReverseStep = 6.7f;
        float nWalksConstant = (float) (24 * Math.log(1.0e6));
    }

    private static final String[] FEAT_NAMES = new String[]{"ppr"};

    static DirectedGraph buildGraph(Dataset model, AlignmentSet prealign, Object2IntMap<Resource> identifiers) {
        DirectedGraph g = new DirectedGraph();
        StmtIterator stat = model.listStatements();
        while (stat.hasNext()) {
            Statement s = stat.next();
            if (s.getObject().isResource()) {
                final int i, j;
                if (identifiers.containsKey(s.getSubject())) {
                    i = identifiers.getInt(s.getSubject());
                } else {
                    i = g.addNode();
                    identifiers.put(s.getSubject(), i);
                }
                if (identifiers.containsKey(s.getObject().asResource())) {
                    j = identifiers.getInt(s.getObject().asResource());
                } else {
                    j = g.addNode();
                    identifiers.put(s.getObject().asResource(), j);
                }
                g.addEdge(i, j);
            }
        }
        for (Alignment a : prealign) {
            if (a.probability > 0) {
                Resource entity1 = a.entity1.toJena(model);
                Resource entity2 = a.entity2.toJena(model);
                final int i, j;
                if (identifiers.containsKey(entity1)) {
                    i = identifiers.getInt(entity1);
                } else {
                    i = g.addNode();
                    identifiers.put(entity1, i);
                }
                if (identifiers.containsKey(entity2)) {
                    j = identifiers.getInt(entity2);
                } else {
                    j = g.addNode();
                    identifiers.put(entity2, j);
                }
                g.addEdge(i, j);
            }

        }
        return g;
    }

    private final static class PPRImpl implements GraphFeature {

        private final DirectedGraph graph;
        private final Object2IntMap<Resource> identifiers;
        private final FastPPRConfiguration pprConfig;
        private final Dataset dataset;

        public PPRImpl(DirectedGraph graph, Object2IntMap<Resource> identifiers, FastPPRConfiguration pprConfig, Dataset dataset) {
            this.graph = graph;
            this.identifiers = identifiers;
            this.pprConfig = pprConfig;
            this.dataset = dataset;
        }

        @Override
        public String id() {
            return "ppr";
        }

        @Override
        public Feature[] extractFeatures(URIRes entity1, URIRes entity2, NaiscListener log) {
            int i = identifiers.getInt(entity1.toJena(dataset));
            int j = identifiers.getInt(entity2.toJena(dataset));
            return Feature.mkArray(new double[]{FastPPR.estimatePPR(graph, i, j, pprConfig)}, FEAT_NAMES);
        }
    }
}
