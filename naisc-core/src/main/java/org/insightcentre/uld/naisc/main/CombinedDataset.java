package org.insightcentre.uld.naisc.main;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.NodeIteratorImpl;
import org.apache.jena.rdf.model.impl.ResIteratorImpl;
import org.apache.jena.rdf.model.impl.StmtIteratorImpl;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.util.Option;

import java.net.URL;
import java.util.Iterator;

public class CombinedDataset implements Dataset {
    private final Dataset dataset1, dataset2;
    private final String id;

    public CombinedDataset(Dataset dataset1, Dataset dataset2) {
        this.dataset1 = dataset1;
        this.dataset2 = dataset2;
        this.id = dataset1.id() + "+" + dataset2.id();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Option<URL> asEndpoint() {
        throw new UnsupportedOperationException("Cannot convert merged dataset into endpoint");
    }

    @Override
    public ResIterator listSubjects() {
        return new ResIteratorImpl(new PairIterator<>(dataset1.listSubjects(), dataset2.listSubjects()));
    }

    @Override
    public Property createProperty(String uri) {
        return dataset1.createProperty(uri);
    }

    @Override
    public Resource createResource(String uri) {
        return dataset2.createResource(uri);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property createProperty) {
        return new ResIteratorImpl(new PairIterator<>(dataset1.listSubjectsWithProperty(createProperty),
            dataset2.listSubjectsWithProperty(createProperty)));
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property createProperty, RDFNode object) {
        return new ResIteratorImpl(new PairIterator<>(dataset1.listSubjectsWithProperty(createProperty, object),
                dataset2.listSubjectsWithProperty(createProperty, object)));
    }

    @Override
    public NodeIterator listObjectsOfProperty(Resource r, Property createProperty) {
        return new NodeIteratorImpl(new PairIterator<>(dataset1.listObjectsOfProperty(r, createProperty),
            dataset2.listObjectsOfProperty(r, createProperty)), null);
    }

    @Override
    public StmtIterator listStatements(Resource source, Property prop, RDFNode rdfNode) {
        return new StmtIteratorImpl(new PairIterator<>(dataset1.listStatements(source, prop, rdfNode),
            dataset2.listStatements(source, prop, rdfNode)));
    }

    @Override
    public StmtIterator listStatements() {

        return new StmtIteratorImpl(new PairIterator<>(dataset1.listStatements(), dataset2.listStatements()));
    }

    @Override
    public QueryExecution createQuery(Query query) {
        throw new UnsupportedOperationException("Cannot query combined dataset");
    }

    private static class PairIterator<X> implements Iterator<X> {
        private final Iterator<X> iter1, iter2;

        public PairIterator(Iterator<X> iter1, Iterator<X> iter2) {
            this.iter1 = iter1;
            this.iter2 = iter2;
        }

        @Override
        public boolean hasNext() {
            return iter1.hasNext() || iter2.hasNext();
        }

        @Override
        public X next() {
            if(iter1.hasNext()) {
                return iter1.next();
            } else {
                return iter2.next();
            }
        }
    }
}
