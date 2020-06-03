package org.insightcentre.uld.naisc.blocking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.SimpleCache;
import org.insightcentre.uld.naisc.util.URI2Label;

/**
 * Pre-linking assumes identical values of a property and produces any exact
 * pre-matches
 *
 * @author John McCrae
 */
public class Prelinking {

    private final Set<Pair<String, String>> prelinkProperties;

    /**
     * Create a prelinking
     *
     * @param prelinkProperties The properties to prelink on
     */
    public Prelinking(Set<Pair<String, String>> prelinkProperties) {
        this.prelinkProperties = prelinkProperties;
    }

    /**
     * Find any pairs that can be easily linked together (as they have the same
     * value for some fairly unique property)
     *
     * @param left The left resource
     * @param right The right resource
     * @param log The logger
     * @return A list of pairs of resources that are pre-linked
     */
    public Set<Pair<Resource, Resource>> prelink(Dataset left, Dataset right, NaiscListener log) {
        Set<Pair<Resource, Resource>> prelinks = new HashSet<>();
        for (Pair<String, String> p : prelinkProperties) {
            SimpleCache<String, String> labelCache = new SimpleCache<>(10000);

            String leftUri = p._1;
            String rightUri = p._2;
            if (leftUri.equals("")) {
                if (rightUri.equals("")) {
                    Map<String, Set<Resource>> leftByLabel = buildLeftByLabel(left.listSubjects());
                    buildRightByLabel(right.listSubjects(), leftByLabel, prelinks);
                } else {
                    final ResIterator iter = left.listSubjects();
                    while (iter.hasNext()) {
                        Resource l = iter.next();
                        String leftLabel = labelCache.get(l.getURI(), e -> URI2Label.fromURI(e));
                        if (l.isURIResource()) {
                            final ResIterator iter2;
                            iter2 = right.listSubjectsWithProperty(right.createProperty(rightUri));
                            while (iter2.hasNext()) {
                                Resource r = iter2.next();
                                String rightLabel = labelCache.get(r.getURI(), e -> URI2Label.fromURI(e));
                                if (r.isURIResource()
                                        && l.isURIResource() && leftLabel.equals(rightLabel)) {
                                    prelinks.add(new Pair<>(l, r));
                                }
                            }
                        }
                    }
                }
            } else {
                final StmtIterator iter = left.listStatements(null, left.createProperty(leftUri), (RDFNode) null);
                while (iter.hasNext()) {
                    Statement s = iter.next();
                    Resource l = s.getSubject();

                    if (l.isURIResource()) {
                        final ResIterator iter2;
                        if (rightUri.equals("")) {
                            iter2 = right.listSubjects();
                        } else {
                            iter2 = right.listSubjectsWithProperty(right.createProperty(rightUri), s.getObject());
                        }
                        while (iter2.hasNext()) {
                            Resource r = iter2.next();
                            if (r.isURIResource()) {
                                prelinks.add(new Pair<>(l, r));
                            }
                        }
                    }
                }

            }
        }
        return prelinks;
    }

    public static Set<Resource> leftPrelinked(Set<Pair<Resource, Resource>> s) {
        return s.stream().map(x -> x._1).collect(Collectors.toSet());
    }

    public static Set<Resource> rightPrelinked(Set<Pair<Resource, Resource>> s) {
        return s.stream().map(x -> x._2).collect(Collectors.toSet());
    }

    private Map<String, Set<Resource>> buildLeftByLabel(ResIterator subjects) {
        Map<String, Set<Resource>> leftByLabel = new HashMap<>();
        while(subjects.hasNext()) {
            Resource r = subjects.next();
            String label = URI2Label.fromURI(r.getURI());
            if(!leftByLabel.containsKey(label)) {
                leftByLabel.put(label, new HashSet<>());
            }
            leftByLabel.get(label).add(r);
        }
        return leftByLabel;
    }

    private void buildRightByLabel(ResIterator subjects, Map<String, Set<Resource>> leftByLabel, Set<Pair<Resource, Resource>> prelinks) {
        while(subjects.hasNext()) {
            Resource r = subjects.next();
            String label = URI2Label.fromURI(r.getURI());
            if(leftByLabel.containsKey(label)) {
                for(Resource l : leftByLabel.get(label)) {
                    if(l.isURIResource() && r.isURIResource()) {
                        prelinks.add(new Pair<>(l,r));
                    }
                }
            }
        }
    }
}
