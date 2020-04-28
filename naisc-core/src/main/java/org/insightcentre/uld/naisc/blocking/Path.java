package org.insightcentre.uld.naisc.blocking;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.lens.Label;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Blocking strategy that uses the path distance to decide if two entities are
 * related
 *
 * @author John McCrae
 */
public class Path implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> analysis, NaiscListener listener) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if (config.maxMatches <= 0) {
            throw new IllegalArgumentException("Must have at least one match");
        }
        if (config.preblockLeftProperty == null) {
            throw new IllegalArgumentException("Property for preblocking cannot be null");
        }
        return new PathImpl(new Prelinking(Collections.singleton(new Pair<>(config.preblockLeftProperty,
                config.preblockRightProperty == null || config.preblockRightProperty.equals("") ? config.preblockLeftProperty : config.preblockRightProperty))), config.maxMatches);
    }

    public static class Configuration {

        /**
         * The maximum number of nodes to explore in the path method
         */
        @ConfigurationParameter(description="The maximum number of nodes to explore in the path method")
        public int maxMatches = 100;

        /**
         * The property to use in the left side of the pre-blocking
         */
        @ConfigurationParameter(description = "The property to use in the left side of the pre-blocking")
        public String preblockLeftProperty = Label.RDFS_LABEL;

        /**
         * The property to use in the right side of the pre-blocking (or empty for same as left)
         */
        @ConfigurationParameter(description = "The property to use in the right side of the pre-blocking (or empty for same as left)")
        public String preblockRightProperty = null;
    }

    private static class PathImpl implements BlockingStrategy {

        private final Prelinking preblocking;
        private final int maxMatches;

        public PathImpl(Prelinking preblocking, int maxMatches) {
            this.preblocking = preblocking;
            this.maxMatches = maxMatches;
        }

        @Override
        public Collection<Blocking> block(Dataset left, Dataset right, NaiscListener log) {
            Map<Resource, List<Resource>> prelinking = convertPrelinking(preblocking.prelink(left, right, log));
            return new AbstractCollection<Blocking>() {
                @Override
                public Iterator<Blocking> iterator() {
                    ResIterator leftIter = left.listSubjects();
                    return new PathIterator(leftIter, left, right, prelinking, maxMatches);
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private Map<Resource, List<Resource>> convertPrelinking(Set<Pair<Resource, Resource>> prelinking) {
            Map<Resource, List<Resource>> map = new HashMap<>();
            for (Pair<Resource, Resource> a : prelinking) {
                if (!map.containsKey(a._1)) {
                    map.put(a._1, new ArrayList<>());
                }
                map.get(a._1).add(a._2);
            }
            return map;
        }

    }

    private static class PathIterator implements Iterator<Blocking> {

        private final ResIterator leftIter;
        private final Dataset left, right;
        private final Map<Resource, List<Resource>> prelinking;
        private Iterator<Pair<Resource, Resource>> nodesIter = null;
        private final int maxMatches;

        public PathIterator(ResIterator leftIter, Dataset left, Dataset right, Map<Resource, List<Resource>> prelinking, int maxMatches) {
            this.leftIter = leftIter;
            this.left = left;
            this.right = right;
            this.prelinking = prelinking;
            this.maxMatches = maxMatches;
            findNext();
        }

        private void findNext() {
            while (leftIter.hasNext()) {
                Resource l = leftIter.next();
                if(!l.isURIResource())
                    continue;
                List<Pair<Resource, Resource>> nodes = new ArrayList<>();
                Set<Resource> leftToExpand = new HashSet<>();
                Set<Resource> leftVisited = new HashSet<>();
                Set<Resource> rightToExpand = new HashSet<>();
                Set<Resource> rightVisited = new HashSet<>();
                leftToExpand.add(l);
                while ((!leftToExpand.isEmpty() || !rightToExpand.isEmpty()) && nodes.size() < maxMatches) {
                    Set<Resource> lte2 = new HashSet<>(), rte2 = new HashSet<>();
                    for (Resource r : leftToExpand) {
                        StmtIterator stIter = left.listStatements(r, null, null);
                        while (stIter.hasNext()) {
                            Statement s = stIter.next();
                            if (s.getObject().isResource() && !leftVisited.contains(s.getObject().asResource())) {
                                lte2.add(s.getObject().asResource());
                            }
                        }
                        stIter = left.listStatements(null, null, r);
                        while (stIter.hasNext()) {
                            Statement s = stIter.next();
                            if (!leftVisited.contains(s.getSubject())) {
                                lte2.add(s.getSubject());
                            }
                        }
                        if (prelinking.containsKey(r)) {
                            nodes.addAll(prelinking.get(r).stream().map(r2 -> new Pair<>(l, r2)).collect(Collectors.toList()));
                            rte2.addAll(prelinking.get(r));
                        }
                    }
                    for (Resource r : rightToExpand) {
                        StmtIterator stIter = right.listStatements(r, null, null);
                        while (stIter.hasNext()) {
                            Statement s = stIter.next();
                            if (s.getObject().isResource() && !rightVisited.contains(s.getObject().asResource())) {
                                rte2.add(s.getObject().asResource());
                                nodes.add(new Pair<>(l, s.getObject().asResource()));
                            }
                        }stIter = right.listStatements(null, null, r);
                        while (stIter.hasNext()) {
                            Statement s = stIter.next();
                            if (!rightVisited.contains(s.getSubject())) {
                                rte2.add(s.getSubject());
                                nodes.add(new Pair<>(l, s.getSubject()));
                            }
                        }
                    }
                    leftVisited.addAll(leftToExpand);
                    rightVisited.addAll(rightToExpand);
                    leftToExpand = lte2;
                    rightToExpand = rte2;
                }
                if (!nodes.isEmpty()) {
                    if(nodes.size() > maxMatches) 
                        nodes.subList(maxMatches, nodes.size()).clear();
                    nodesIter = nodes.iterator();
                    return;
                }
            }
            nodesIter = null;
        }

        @Override
        public boolean hasNext() {
            return nodesIter != null;
        }

        @Override
        public Blocking next() {
            while (nodesIter != null && !nodesIter.hasNext()) {
                findNext();
            }
            if (nodesIter != null) {
                Pair<Resource, Resource> next = nodesIter.next();
                while (nodesIter != null && !nodesIter.hasNext()) {
                    findNext();
                }
                return new Blocking(next._1, next._2, left.id(), right.id());
            } else {
                throw new NoSuchElementException();
            }
        }

    }
}
