package org.insightcentre.uld.naisc.matcher;

import java.util.*;

import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Blocking;
import org.insightcentre.uld.naisc.Dataset;
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
        Map<Resource, Resource> l2r = new HashMap<>();
        Map<Resource, Resource> r2l = new HashMap<>();
        Set<Resource> lburnt = new HashSet<>();
        Set<Resource> rburnt = new HashSet<>();
        for(Blocking block : blocking) {
            Resource block1 = block.asJena1(left), block2 = block.asJena2(right);
            if(l2r.containsKey(block1) || r2l.containsKey(block2)) {
                l2r.remove(block1);
                r2l.remove(block2);
                lburnt.add(block1);
                rburnt.add(block2);
            } else if(!lburnt.contains(block1) && !rburnt.contains(block2)) {
                l2r.put(block1, block2);
                r2l.put(block2, block1);
            }
        }
        for(Map.Entry<Resource, Resource> e : l2r.entrySet()) {
            prematch.add(new Alignment(e.getKey(), e.getValue(), 1.0));
        }
        return prematch;
    }
}
