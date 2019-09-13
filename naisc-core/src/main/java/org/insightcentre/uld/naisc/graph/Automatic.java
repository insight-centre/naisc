package org.insightcentre.uld.naisc.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.GraphFeatureFactory;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.MatchResult;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Automatically construct features from the graph
 * 
 * @author John McCrae
 */
public class Automatic implements GraphFeatureFactory {

    @Override
    public GraphFeature makeFeature(Dataset sparqlData, Map<String, Object> params,
            Lazy<Analysis> analysis, Lazy<AlignmentSet> prelinking, NaiscListener listener) {
        listener.message(NaiscListener.Stage.INITIALIZING, NaiscListener.Level.INFO, "Automatically configuring graph features");
        Analysis _analysis = analysis.get();
        List<Pair<String,String>> propMatches = new ArrayList<>();
        for(MatchResult mr : _analysis.matching) {
            if(mr.coversData() && !mr.leftUri.equals("") && !mr.rightUri.equals("")) 
                propMatches.add(new Pair<>(mr.leftUri, mr.rightUri));
        }
        if(!propMatches.isEmpty()) {
            StringBuilder sb = new StringBuilder("Using the following properties as values matches: \n");
            
            for(Pair<String,String>  propMatch : propMatches) {
                sb.append(String.format("%s <-> %s\n", propMatch._1, propMatch._2));
                
            }
            listener.message(NaiscListener.Stage.INITIALIZING, NaiscListener.Level.INFO, sb.toString());
        }
        boolean pprAnalysis = false;
        if(_analysis.isWellConnected()) {
            AlignmentSet as = prelinking.get();
            if(as.size() > 10) {
                pprAnalysis = true;
            }
            
        }
        GraphFeature ppr = null;
        if(pprAnalysis) {
            ppr = new PPR().makeFeature(sparqlData, params, analysis, prelinking, listener);
            listener.message(NaiscListener.Stage.INITIALIZING, NaiscListener.Level.INFO, "Using PPR on graph");
        }
        return new AutomaticImpl(propMatches, ppr);
    }

    public static class Configuration {
        
    }
    
    private static class AutomaticImpl implements GraphFeature {
        final List<Pair<String,String>> propMatches;
        final GraphFeature pprAnalysis;
        final String[] featureNames;

        public AutomaticImpl(List<Pair<String, String>> propMatches, GraphFeature pprAnalysis) {
            this.propMatches = propMatches;
            this.pprAnalysis = pprAnalysis;
            List<String> fn = new ArrayList<>();
            for(Pair<String,String> propMatch : propMatches) {
                fn.add(propMatch._1 + "-" + propMatch._2);
            }
            if(pprAnalysis != null)
                fn.add("ppr");
            this.featureNames = fn.toArray(new String[fn.size()]);
        }

        @Override
        public String id() {
            return "auto";
        }

        @Override
        public double[] extractFeatures(Resource entity1, Resource entity2, NaiscListener log) {
            double[] features = new double[featureNames.length];
            int i = 0;
            PROP_MATCH:
            for(Pair<String,String> propMatch : propMatches) {
                Model left = entity1.getModel();
                Model right = entity2.getModel();
                final StmtIterator iter1 = entity1.listProperties(left.createProperty(propMatch._1));
                Set<RDFNode> vals1 = new HashSet<>();
                while(iter1.hasNext()) {
                    vals1.add(iter1.next().getObject());
                }
                final StmtIterator iter2 = entity2.listProperties(right.createProperty(propMatch._2));
                while(iter2.hasNext()) {
                    if(vals1.contains(iter2.next().getObject())) {
                        features[i++] = 1.0;
                        continue PROP_MATCH;
                    }
                            
                }
                i++;
            }
            if(pprAnalysis != null)
                features[i] = pprAnalysis.extractFeatures(entity1, entity2, log)[0];
            return features;
        }

        
        
        @Override
        public String[] getFeatureNames() {
            return featureNames;
        }
        
    }

    
}
