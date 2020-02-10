package org.insightcentre.uld.naisc.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.GraphFeatureFactory;
import org.insightcentre.uld.naisc.NaiscListener;
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
    public GraphFeature makeFeature(Dataset sparqlData, Map<String, Object> params, Lazy<Analysis> analysis, Lazy<AlignmentSet> prelinking, NaiscListener listener) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        Object2IntMap<Resource> identifiers = new Object2IntOpenHashMap<>();
        DirectedGraph graph = buildGraph(sparqlData, prelinking.get(), identifiers);

        FastPPRConfiguration pprConfig = new FastPPRConfiguration(config.pprSignificanceThreshold, config.reversePPRApproximationFactor, config.teleportProbability, config.forwardStepsPerReverseStep, config.nWalksConstant);

        return new PPRImpl(graph, identifiers, pprConfig);
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
            if (a.score > 0) {
                final int i, j;
                if (identifiers.containsKey(a.entity1)) {
                    i = identifiers.getInt(a.entity1);
                } else {
                    i = g.addNode();
                    identifiers.put(a.entity1, i);
                }
                if (identifiers.containsKey(a.entity2)) {
                    j = identifiers.getInt(a.entity2);
                } else {
                    j = g.addNode();
                    identifiers.put(a.entity2, j);
                }
                g.addEdge(i, j);
            }

        }
        return g;
    }

    private final class PPRImpl implements GraphFeature {

        private final DirectedGraph graph;
        private final Object2IntMap<Resource> identifiers;
        private final FastPPRConfiguration pprConfig;

        public PPRImpl(DirectedGraph graph, Object2IntMap<Resource> identifiers, FastPPRConfiguration pprConfig) {
            this.graph = graph;
            this.identifiers = identifiers;
            this.pprConfig = pprConfig;
        }

        @Override
        public String id() {
            return "ppr";
        }

        @Override
        public double[] extractFeatures(Resource entity1, Resource entity2, NaiscListener log) {
            int i = identifiers.getInt(entity1);
            int j = identifiers.getInt(entity2);
            return new double[]{FastPPR.estimatePPR(graph, i, j, pprConfig)};
        }

        @Override
        public String[] getFeatureNames() {
            return FEAT_NAMES;
        }

    }
}
