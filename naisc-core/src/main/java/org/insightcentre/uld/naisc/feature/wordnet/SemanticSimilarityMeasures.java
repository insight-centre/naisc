package org.insightcentre.uld.naisc.feature.wordnet;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Measures of semantic similarity based on WordNet
 * @author John McCrae
 */
public class SemanticSimilarityMeasures {
    private final Map<String, Set<Synset>> roots;
    private final Object2IntMap<Synset> maxDepths;
    private final WordNetData wordnet;

    public SemanticSimilarityMeasures(WordNetData wordnet) {
        this.wordnet = wordnet;
        this.roots = wordnet.findRoots();
        this.maxDepths = new Object2IntOpenHashMap<>();
        for(Set<Synset> ss : roots.values()) {
            for(Synset s : ss) {
                maxDepths.put(s, wordnet.maxDepth(s));
            }
        }
    }

    

    public double shortestPath(Synset s1, Synset s2) {
        Synset lso = wordnet.leastCommonSubsumer(s1, s2);
        if(lso != null) {
            int maxDepth = maxDepths.get(wordnet.findRoot(lso));
            double score = (2.0 * maxDepth - 
                (wordnet.depthTo(s1, lso) + wordnet.depthTo(s2, lso))) / 2.0 / maxDepth;
            return score;
        } else {
            return 0;
        }
    }

    public double wuPalmer(Synset s1, Synset s2) {
        Synset lso = wordnet.leastCommonSubsumer(s1, s2);
        if(lso != null) {
            int lsoDepth = wordnet.depth(lso);
            return 2.0 * lsoDepth /
                (wordnet.depthTo(s1, lso) + wordnet.depthTo(s2, lso) + 2.0 * lsoDepth);
        } else {
            return 0;
        }
 
    }

    public double leakcockChodorow(Synset s1, Synset s2) {
        Synset lso = wordnet.leastCommonSubsumer(s1, s2);
        if(lso != null) {
            int maxDepth = maxDepths.get(wordnet.findRoot(lso));
            return - Math.log( (double)(wordnet.depthTo(s1, lso) + wordnet.depthTo(s2, lso) + 1) / (2.0 * maxDepth + 1));
        } else {
            return 0;
        }
    }

    private static final double alpha = 0.2, beta = 0.6;
    
    public double li(Synset s1, Synset s2) {
        Synset lso = wordnet.leastCommonSubsumer(s1, s2);
        if(lso != null) {
            int len = wordnet.depthTo(s1, lso) + wordnet.depthTo(s2, lso);
            int depth = wordnet.depth(lso);
            return Math.exp(-alpha * len) * Math.tanh(beta * depth);
        } else {
            return 0;
        }
    }

    private double mrScore(double d) {
        return Math.exp(-0.8 * d);
    }
    
    public double modifiedRychalska(Synset s1, Synset s2) {
        if(s1.equals(s2)) {
            return mrScore(0);
        } 
        
        for(Synset.Relation r1 : s1.relations) {
            if(r1.relType.equals("hypernym") || r1.relType.equals("instance_hypernym")) {
                Synset s3 = wordnet.lookupSynset(r1.target);
                if(s2.equals(s3)) {
                    return mrScore(1);
                } else {
                    for(Synset.Relation r2 : s3.relations)  {
                        if(r2.relType.equals("hypernym") || r2.relType.equals("instance_hypernym")) {
                            Synset s4 = wordnet.lookupSynset(r2.target);
                            if(s2.equals(s4)) {
                                return mrScore(2);
                            }
                        }
                    }
                }
            } else if(r1.relType.equals("hyponym") || r1.relType.equals("instance_hyponym")) {
                Synset s3 = wordnet.lookupSynset(r1.target);
                if(s2.equals(s3)) {
                    return mrScore(1);
                } else {
                    for(Synset.Relation r2 : s3.relations)  {
                        if(r2.relType.equals("hyponym") || r2.relType.equals("instance_hyponym")) {
                            Synset s4 = wordnet.lookupSynset(r2.target);
                            if(s2.equals(s4)) {
                                return mrScore(2);
                            }
                        }
                    }
                }
            } else if(r1.relType.equals("similar")) {
                Synset s3 = wordnet.lookupSynset(r1.target);
                if(s2.equals(s3)) {
                    return mrScore(2);
                }
            }
            
        }
        return mrScore(3);
    }
    

}
