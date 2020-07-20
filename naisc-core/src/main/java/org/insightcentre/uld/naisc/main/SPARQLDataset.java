package org.insightcentre.uld.naisc.main;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.NodeIteratorImpl;
import org.apache.jena.rdf.model.impl.ResIteratorImpl;
import org.apache.jena.rdf.model.impl.StmtIteratorImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.util.Option;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.jena.rdfconnection.*;
import org.insightcentre.uld.naisc.util.Some;

public class SPARQLDataset implements Dataset {
    private final Model model = ModelFactory.createDefaultModel();
    private final String endpoint;
    private final String id;

    public SPARQLDataset(String endpoint, String id) {
        this.endpoint = endpoint;
        this.id = id;
    }

    @Override
    public Option<URL> asEndpoint() {
        try {
            return new Some(new URL(endpoint));
        } catch(MalformedURLException x) {
            throw new RuntimeException("SPARQL endpoint URL not valid", x);
        }
    }

    @Override
    public ResIterator listSubjects() {
        try(RDFConnection conn = makeConnection()) {
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
        return model.createProperty(uri);
    }

    @Override
    public Resource createResource(String uri) {
        return model.createResource(uri);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property prop) {
        try(RDFConnection conn = makeConnection()) {
            String queryString = "SELECT DISTINCT ?s { ?s <" +  prop.getURI() + "> ?o }";
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
    public ResIterator listSubjectsWithProperty(Property prop, RDFNode object) {
        try(RDFConnection conn = makeConnection()) {
            String queryString = "SELECT DISTINCT ?s { ?s <" +  prop.getURI() + "> " + toID(object) +" }";
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
    public NodeIterator listObjectsOfProperty(Resource r, Property p) {
        try(RDFConnection conn = makeConnection()) {
            String queryString = "SELECT DISTINCT ?s { " + toID(r) + " <" +  p.getURI() + "> ?o }";
            Query query = QueryFactory.create(queryString);
            final ArrayList<RDFNode> objects = new ArrayList<>();
            try (QueryExecution qexec = conn.query(query)) {
                ResultSet results = qexec.execSelect();
                while(results.hasNext()) {
                    QuerySolution soln = results.next();
                    objects.add(soln.get("o"));
                }
            }
            return new NodeIteratorImpl(objects.iterator(), null /* This is an undocumented parameter that does nothing in the Jena source code */);
        }
    }

    @Override
    public StmtIterator listStatements() {
        try(RDFConnection conn = makeConnection()) {
            String queryString = "SELECT * { ?s ?p ?o }";
            Query query = QueryFactory.create(queryString);
            final ArrayList<Statement> objects = new ArrayList<>();
            try (QueryExecution qexec = conn.query(query)) {
                ResultSet results = qexec.execSelect();
                while(results.hasNext()) {
                    QuerySolution soln = results.next();
                    objects.add(model.createStatement(soln.getResource("s"), model.createProperty(soln.getResource("p").getURI()),
                        soln.get("o")));
                }
            }
            return new StmtIteratorImpl(objects.iterator());
        }
    }

    @Override
    public StmtIterator listStatements(Resource s, Property p, RDFNode o) {
        try(RDFConnection conn = makeConnection()) {
            String queryString = "SELECT * { " +
                    (s == null ? "?s" : toID(s)) + " " +
                    (p == null ? "?p" : toID(p)) + " " +
                    (o == null ? "?o" : toID(o)) + "}";
            Query query = QueryFactory.create(queryString);
            final ArrayList<Statement> objects = new ArrayList<>();
            try (QueryExecution qexec = conn.query(query)) {
                ResultSet results = qexec.execSelect();
                while(results.hasNext()) {
                    QuerySolution soln = results.next();
                    objects.add(model.createStatement(
                    s == null ? soln.getResource("s") : s,
                    p == null ? model.createProperty(soln.getResource("p").getURI()) : p,
                    o == null ? soln.get("o") : o));
                }
            }
            return new StmtIteratorImpl(objects.iterator());
        }
    }

    @Override
    public QueryExecution createQuery(Query query) {
        RDFConnection conn = makeConnection();
        return  conn.query(query);
    }

    @Override
    public String id() {
        return id;
    }

    protected RDFConnection makeConnection() { return RDFConnectionFactory.connect(endpoint); }


    private String toID(RDFNode node) {
        if(node.isLiteral()) {
            Literal l = node.asLiteral();
            if(l.getLanguage() != null) {
                return "\"" + l.getLexicalForm() + "\"@" + l.getLanguage();
            } else if(l.getDatatypeURI() != null) {
                return "\"" + l.getLexicalForm() + "\"^^<" + l.getDatatypeURI() + ">";
            } else {
                return "\"" + l.getLexicalForm() + "\"";
            }
        } else if(node.isURIResource()) {
            return "<" + node.asResource().getURI() + ">";
        } else {
            return "[]";
        }
    }
}

