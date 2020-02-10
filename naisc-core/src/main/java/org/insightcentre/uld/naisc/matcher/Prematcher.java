package org.insightcentre.uld.naisc.matcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * A prematcher is a simple step that looks for any potential matches that are 
 * unique
 * 
 * @author John McCrae
 */
public class Prematcher {

    public AlignmentSet prematch(Iterable<Pair<Resource,Resource>> blocking) {
        AlignmentSet prematch = new AlignmentSet();
        Map<Resource, Resource> l2r = new HashMap<>();
        Map<Resource, Resource> r2l = new HashMap<>();
        Set<Resource> lburnt = new HashSet<>();
        Set<Resource> rburnt = new HashSet<>();
        for(Pair<Resource,Resource> block : blocking) {
            if(l2r.containsKey(block._1) || r2l.containsKey(block._2)) {
                l2r.remove(block._1);
                r2l.remove(block._2);
                lburnt.add(block._1);
                rburnt.add(block._2);
            } else if(!lburnt.contains(block._1) && !rburnt.contains(block._2)) {
                l2r.put(block._1, block._2);
                r2l.put(block._2, block._1);
            }
        }
        for(Map.Entry<Resource, Resource> e : l2r.entrySet()) {
            prematch.add(new Alignment(e.getKey(), e.getValue(), 1.0));
        }
        return prematch;
    }
}
