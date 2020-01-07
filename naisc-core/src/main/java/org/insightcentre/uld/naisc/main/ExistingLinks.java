package org.insightcentre.uld.naisc.main;

import org.apache.jena.rdf.model.*;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Methods for searching for existing links between two datasets
 */
public class ExistingLinks {

    public static Pair<Set<Resource>, Set<Resource>> findPreexisting(List<Scorer> scorers, Dataset left, Dataset right) {
        Set<Resource> leftPreexisting = new HashSet<>(), rightPreexisting = new HashSet<>();
        Set<Property> relations = new HashSet<>();
        for(Scorer scorer : scorers) {
            relations.add(left.createProperty(scorer.relation()));
        }
        Set<Resource> rSubjs = new HashSet<>();
        ResIterator iter = right.listSubjects();
        while(iter.hasNext()) {
            rSubjs.add(iter.nextResource());
        }
        for(Property p : relations) {
            ResIterator liter = left.listSubjectsWithProperty(p);
            while(liter.hasNext()) {
                Resource l = liter.nextResource();
                NodeIterator siter = left.listObjectsOfProperty(l, p);
                while(siter.hasNext()) {
                    RDFNode n = siter.next();
                    if(n.isResource() && rSubjs.contains(n.asResource())) {
                        leftPreexisting.add(l);
                        rightPreexisting.add(n.asResource());
                    }
                }
            }
        }
        return new Pair<>(leftPreexisting, rightPreexisting);
    }

    public static Iterable<Pair<Resource, Resource>> filterBlocking(final Iterable<Pair<Resource, Resource>> blocking,
                                                               final Pair<Set<Resource>, Set<Resource>> preexisting) {
       return new Iterable<Pair<Resource,Resource>>() {
            @Override
            public Iterator<Pair<Resource, Resource>> iterator() {
                return new FilterIterable(blocking.iterator(), preexisting);
           }
       };
    }

    private static class FilterIterable implements Iterator<Pair<Resource, Resource>> {
        private final Iterator<Pair<Resource, Resource>> iter;
        private Pair<Resource,Resource> next;
        private final Pair<Set<Resource>, Set<Resource>> preexisting;

        public FilterIterable(Iterator<Pair<Resource, Resource>> iter, Pair<Set<Resource>, Set<Resource>> preexisting) {
            this.iter = iter;
            this.preexisting = preexisting;
            advance();
        }

        private void advance() {
            if(!iter.hasNext()) {
                next = null;
                return;
            }
            next = iter.next();
            while(preexisting._1.contains(next._1) || preexisting._2.contains(next._2)) {
                if(!iter.hasNext()) {
                    next = null;
                    return;
                }
                next = iter.next();
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Pair<Resource, Resource> next() {
            Pair<Resource, Resource> r = next;
            advance();
            return r;
        }
    }
}
