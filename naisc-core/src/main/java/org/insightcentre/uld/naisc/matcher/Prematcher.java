package org.insightcentre.uld.naisc.matcher;

import java.util.*;

import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * A prematcher is a simple step that looks for any potential matches that are 
 * unique
 * 
 * @author John McCrae
 */
public class Prematcher {

    public AlignmentSet prematch(Collection<Blocking> blocking, Dataset left, Dataset right) {
        AlignmentSet prematch = new AlignmentSet();
        Map<URIRes, URIRes> l2r = new HashMap<>();
        Map<URIRes, URIRes> r2l = new HashMap<>();
        Set<URIRes> lburnt = new HashSet<>();
        Set<URIRes> rburnt = new HashSet<>();
        for(Blocking block : blocking) {
            if(l2r.containsKey(block.entity1) || r2l.containsKey(block.entity2)) {
                l2r.remove(block.entity1);
                r2l.remove(block.entity2);
                lburnt.add(block.entity1);
                rburnt.add(block.entity2);
            } else if(!lburnt.contains(block.entity1) && !rburnt.contains(block.entity2)) {
                l2r.put(block.entity1, block.entity2);
                r2l.put(block.entity2, block.entity1);
            }
        }
        for(Map.Entry<URIRes, URIRes> e : l2r.entrySet()) {
            prematch.add(new Alignment(e.getKey(), e.getValue(), 1.0));
        }
        return prematch;
    }
}
