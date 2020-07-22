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
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.jena.rdfconnection.*;
import org.insightcentre.uld.naisc.util.Some;

public class SPARQLDataset implements Dataset {
    private final Model model = ModelFactory.createDefaultModel();
    private final String endpoint;
    private final String id;
    private final int limit;

    public SPARQLDataset(String endpoint, String id, int limit) {
        this.endpoint = endpoint;
        this.id = id;
        this.limit = limit;
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
        String queryString = "SELECT DISTINCT ?s { ?s ?p ?o }";
        return new ResIteratorImpl(new OffsetLimitSPARQL<Resource>(queryString, limit) {
            @Override
            protected Resource makeResult(QuerySolution soln) {
                return soln.getResource("s");
            }
        });
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
        String queryString = "SELECT DISTINCT ?s { ?s <" +  prop.getURI() + "> ?o }";
        return new ResIteratorImpl(new OffsetLimitSPARQL<Resource>(queryString, limit) {
            @Override
            protected Resource makeResult(QuerySolution soln) {
                return soln.getResource("s");
            }
        });
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property prop, RDFNode object) {
        String queryString = "SELECT DISTINCT ?s WHERE { ?s <" +  prop.getURI() + "> " + toID(object) +" }";
        return new ResIteratorImpl(new OffsetLimitSPARQL<Resource>(queryString, limit) {
            @Override
            protected Resource makeResult(QuerySolution soln) {
                return soln.getResource("s");
            }
        });
    }

    @Override
    public NodeIterator listObjectsOfProperty(Resource r, Property p) {
        String queryString = "SELECT DISTINCT ?o { " + toID(r) + " <" +  p.getURI() + "> ?o }";
        return new NodeIteratorImpl(new OffsetLimitSPARQL<RDFNode>(queryString, limit) {
            @Override
            protected RDFNode makeResult(QuerySolution soln) {
                return soln.get("o");
            }
        }, null /* This is an undocumented parameter that does nothing in the Jena source code */);
    }

    @Override
    public StmtIterator listStatements() {
        String queryString = "SELECT * { ?s ?p ?o }";
        return new StmtIteratorImpl(new OffsetLimitSPARQL<Statement>(queryString, limit) {
            @Override
            protected Statement makeResult(QuerySolution soln) {
                return model.createStatement(soln.getResource("s"), model.createProperty(soln.getResource("p").getURI()),
                        soln.get("o"));
            }
        });
    }

    @Override
    public StmtIterator listStatements(Resource s, Property p, RDFNode o) {
        String queryString = "SELECT * { " +
                (s == null ? "?s" : toID(s)) + " " +
                (p == null ? "?p" : toID(p)) + " " +
                (o == null ? "?o" : toID(o)) + "}";
        return new StmtIteratorImpl(new OffsetLimitSPARQL<Statement>(queryString, 20) {
            @Override
            protected Statement makeResult(QuerySolution soln) {
                return model.createStatement(
                        s == null ? soln.getResource("s") : s,
                        p == null ? model.createProperty(soln.getResource("p").getURI()) : p,
                        o == null ? soln.get("o") : o);
            }
        });
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

    private abstract class OffsetLimitSPARQL<A> implements Iterator<A> {
        private final String queryString;
        private int offset = 0;
        private final int limit;
        private boolean done = false;
        private int index = 0;

        public OffsetLimitSPARQL(String queryString, int limit) {
            this.queryString = queryString;
            this.limit = limit;
            advance();
        }

        private ArrayList<A> resultCache = new ArrayList<>();

        private void advance() {
            try(RDFConnection conn = makeConnection()) {
                final A last;
                if(!resultCache.isEmpty()) {
                    last = resultCache.get(resultCache.size() - 1);
                } else {
                    last = null;
                }
                resultCache.clear();
                index = 0;
                if(last != null) {
                    resultCache.add(last);
                }
                done = true;
                Query query = QueryFactory.create(queryString + " LIMIT " + limit + " OFFSET " + offset);
                offset += limit;
                try (QueryExecution qexec = conn.query(query)) {
                    ResultSet results = qexec.execSelect();
                    while(results.hasNext()) {
                        QuerySolution soln = results.next();
                        resultCache.add(makeResult(soln));
                        done = false;
                    }
                }
            }

        }

        protected abstract A makeResult(QuerySolution soln);

        @Override
        public boolean hasNext() {
            return !done || index < resultCache.size();
        }

        @Override
        public A next() {
            if(done && index >= resultCache.size()) {
                throw new NoSuchElementException();
            }
            if(index == resultCache.size() - 1) {
                advance();
            }
            return resultCache.get(index++);
        }
    }
}
