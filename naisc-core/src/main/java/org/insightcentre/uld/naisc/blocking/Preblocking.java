package org.insightcentre.uld.naisc.blocking;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.URI2Label;

/**
 * Pre-blocking assumes identical values of a property and produces any exact
 * pre-matches
 *
 * @author John McCrae
 */
public class Preblocking {

    private final Set<Pair<String, String>> preblockProperties;

    /**
     * Create a preblocking
     *
     * @param preblockProperties The properties to preblock on
     */
    public Preblocking(Set<Pair<String, String>> preblockProperties) {
        this.preblockProperties = preblockProperties;
    }

    /**
     * Find any pairs that can be easily blocked together (as they have the same
     * value for some fairly unique property)
     *
     * @param left The left resource
     * @param right The right resource
     * @param log The logger
     * @return A list of pairs of resources that are pre-blocked
     */
    public Set<Pair<Resource, Resource>> preblock(Dataset left, Dataset right, NaiscListener log) {
        Set<Pair<Resource, Resource>> preblocks = new HashSet<>();
        for (Pair<String, String> p : preblockProperties) {
            String leftUri = p._1;
            String rightUri = p._2;
            if(leftUri.equals("")) {
                final ResIterator iter = left.listSubjects();
                while (iter.hasNext()) {
                    Resource l = iter.next();
                    if (l.isURIResource()) {
                        final ResIterator iter2;
                        if(rightUri.equals("")) {
                            iter2 = right.listSubjects();
                        } else {
                            iter2 = right.listSubjectsWithProperty(right.createProperty(rightUri));
                        }
                        while (iter2.hasNext()) {
                            Resource r = iter2.next();
                            if (r.isURIResource() &&
                                    !l.isURIResource() || URI2Label.fromURI(l.getURI()).equals(URI2Label.fromURI(r.getURI()))) {
                                preblocks.add(new Pair<>(l, r));
                            }
                        }
                    }
                }
            } else {
                final StmtIterator iter = left.listStatements(null, left.createProperty(leftUri), (RDFNode)null);
                while (iter.hasNext()) {
                    Statement s = iter.next();
                    Resource l = s.getSubject();
                    
                    if (l.isURIResource()) {
                        final ResIterator iter2;
                        if(rightUri.equals("")) {
                            iter2 = right.listSubjects();
                        } else {
                            iter2 = right.listSubjectsWithProperty(right.createProperty(rightUri), s.getObject());
                        }
                        while (iter2.hasNext()) {
                            Resource r = iter2.next();
                            if (r.isURIResource() ) {
                                preblocks.add(new Pair<>(l, r));
                            }
                        }
                    }
                }
                
            }
        }
        return preblocks;
    }

    public static Set<Resource> leftPreblocked(Set<Pair<Resource, Resource>> s) {
        return s.stream().map(x -> x._1).collect(Collectors.toSet());
    }

    public static Set<Resource> rightPreblocked(Set<Pair<Resource, Resource>> s) {
        return s.stream().map(x -> x._2).collect(Collectors.toSet());
    }
}
