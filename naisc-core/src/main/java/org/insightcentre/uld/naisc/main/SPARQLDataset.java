package org.insightcentre.uld.naisc.main;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResIteratorImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.util.Option;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.jena.rdfconnection.*;

public class SPARQLDataset implements Dataset {
    private final Model model = ModelFactory.createDefaultModel();
    private final String endpoint;

    public SPARQLDataset(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Option<URL> asEndpoint() {
        return null;
    }

    @Override
    public ResIterator listSubjects() {
        try(RDFConnection conn = RDFConnectionFactory.connect(endpoint)) {
            String queryString = "SELECT DISTINCT ?s { ?s ?p ?o }";
            Query query = QueryFactory.create(queryString);
            final ArrayList<Resource> subjects = new ArrayList<>();
            try (QueryExecution qexec = conn.query(query)) {
                ResultSet results = qexec.execSelect();
                while(results.hasNext()) {
                    QuerySolution soln = results.next();
                    subjects.add(soln.getResource("s"));
                }
            }
            return new ResIteratorImpl(subjects.iterator());
        }
    }

    @Override
    public Property createProperty(String uri) {
        return null;
    }

    @Override
    public Resource createResource(String uri) {
        return null;
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property createProperty) {
        return null;
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property createProperty, RDFNode object) {
        return null;
    }

    @Override
    public NodeIterator listObjectsOfProperty(Resource r, Property createProperty) {
        return null;
    }

    @Override
    public StmtIterator listStatements(Resource source, Property prop, RDFNode rdfNode) {
        return null;
    }

    @Override
    public StmtIterator listStatements() {
        return null;
    }

    @Override
    public QueryExecution createQuery(Query query) {
        return null;
    }

    @Override
    public String id() {
        return null;
    }
}
