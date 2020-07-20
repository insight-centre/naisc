package org.insightcentre.uld.naisc.main;

import org.apache.jena.rdf.model.*;
import org.insightcentre.uld.naisc.Blocking;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Methods for searching for existing links between two datasets
 */
public class ExistingLinks {

    public static Pair<Set<URIRes>, Set<URIRes>> findPreexisting(Dataset left, Dataset right) {
        Set<URIRes> leftPreexisting = new HashSet<>(), rightPreexisting = new HashSet<>();
        Set<URIRes> rSubjs = new HashSet<>();
        ResIterator iter = right.listSubjects();
        while(iter.hasNext()) {
            rSubjs.add(URIRes.fromJena(iter.nextResource(), right.id()));
        }
        StmtIterator stiter = left.listStatements();
        while(stiter.hasNext()) {
            Statement s = stiter.nextStatement();
            Resource l = s.getSubject();
            Property p = s.getPredicate();
            URIRes l2 = URIRes.fromJena(l, left.id());
            NodeIterator siter = left.listObjectsOfProperty(l, p);
            while(siter.hasNext()) {
                RDFNode n = siter.next();
                if(n.isResource() && rSubjs.contains(URIRes.fromJena(n.asResource(), right.id()))) {
                    leftPreexisting.add(l2);
                    rightPreexisting.add(URIRes.fromJena(n.asResource(), right.id()));
                }
            }
        }
        return new Pair<>(leftPreexisting, rightPreexisting);
    }

    public static Collection<Blocking> filterBlocking(final Collection<Blocking> blocking,
                                                    final Pair<Set<URIRes>, Set<URIRes>> preexisting) {
       return new AbstractCollection<Blocking>() {
            @Override
            public Iterator<Blocking> iterator() {
                return new FilterIterable(blocking.iterator(), preexisting);
           }

           @Override
           public int size() {
               throw new UnsupportedOperationException();
           }
       };
    }

    private static class FilterIterable implements Iterator<Blocking> {
        private final Iterator<Blocking> iter;
        private Blocking next;
        private final Pair<Set<URIRes>, Set<URIRes>> preexisting;

        public FilterIterable(Iterator<Blocking> iter, Pair<Set<URIRes>, Set<URIRes>> preexisting) {
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
            while(preexisting._1.contains(next.entity1) || preexisting._2.contains(next.entity2)) {
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
        public Blocking next() {
            Blocking r = next;
            advance();
            return r;
        }
    }
}
