package org.insightcentre.uld.naisc.main;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.update.UpdateRequest;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SPARQLDatasetTest {
    private SPARQLDataset fromModel(Model model) {
        return new SPARQLDataset("http://www.example.com/example", "test") {
            @Override protected RDFConnection makeConnection() {
                return new RDFConnection() {
                    @Override
                    public void begin(TxnType txnType) {

                    }

                    @Override
                    public void begin(ReadWrite readWrite) {

                    }

                    @Override
                    public boolean promote(Promote promote) {
                        return false;
                    }

                    @Override
                    public void commit() {

                    }

                    @Override
                    public void abort() {

                    }

                    @Override
                    public void end() {

                    }

                    @Override
                    public ReadWrite transactionMode() {
                        return null;
                    }

                    @Override
                    public TxnType transactionType() {
                        return null;
                    }

                    @Override
                    public boolean isInTransaction() {
                        return false;
                    }

                    @Override public QueryExecution query(Query q) {
                        return QueryExecutionFactory.create(q, model);
                    }

                    @Override
                    public void update(UpdateRequest update) {

                    }

                    @Override
                    public void load(String graphName, String file) {

                    }

                    @Override
                    public void load(String file) {

                    }

                    @Override
                    public void load(String graphName, Model model) {

                    }

                    @Override
                    public void load(Model model) {

                    }

                    @Override
                    public void put(String graphName, String file) {

                    }

                    @Override
                    public void put(String file) {

                    }

                    @Override
                    public void put(String graphName, Model model) {

                    }

                    @Override
                    public void put(Model model) {

                    }

                    @Override
                    public void delete(String graphName) {

                    }

                    @Override
                    public void delete() {

                    }

                    @Override
                    public void loadDataset(String file) {

                    }

                    @Override
                    public void loadDataset(Dataset dataset) {

                    }

                    @Override
                    public void putDataset(String file) {

                    }

                    @Override
                    public void putDataset(Dataset dataset) {

                    }

                    @Override
                    public Model fetch(String graphName) {
                        return null;
                    }

                    @Override
                    public Model fetch() {
                        return null;
                    }

                    @Override
                    public Dataset fetchDataset() {
                        return null;
                    }

                    @Override
                    public boolean isClosed() {
                        return false;
                    }

                    @Override
                    public void close() {

                    }

                };
            };
        };
    }


    @Test
    public void testListSubjects() {
        Model m = ModelFactory.createDefaultModel();
        m.add(m.createStatement(m.createResource("http://www.example.com/ex1"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex2"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex4"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex5")));
        SPARQLDataset dataset = fromModel(m);
        List<Resource> results = dataset.listSubjects().toList();
        assertEquals(3, results.size());
        assert(results.contains(m.createResource("http://www.example.com/ex1")));
        assert(results.contains(m.createResource("http://www.example.com/ex2")));
        assert(results.contains(m.createResource("http://www.example.com/ex4")));
    }

    @Test
    public void testListSubjectsWithProperty() {
        Model m = ModelFactory.createDefaultModel();
        m.add(m.createStatement(m.createResource("http://www.example.com/ex1"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex2"), m.createProperty("http://www.example.com/p2"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex4"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex5")));
        SPARQLDataset dataset = fromModel(m);
        List<Resource> results = dataset.listSubjectsWithProperty(m.createProperty("http://www.example.com/p1")).toList();
        assertEquals(2, results.size());
        assert(results.contains(m.createResource("http://www.example.com/ex1")));
        assert(results.contains(m.createResource("http://www.example.com/ex4")));
    }

    @Test
    public void testListSubjectWithProperty2() {
        Model m = ModelFactory.createDefaultModel();
        m.add(m.createStatement(m.createResource("http://www.example.com/ex1"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex2"), m.createProperty("http://www.example.com/p2"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex4"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex5")));
        SPARQLDataset dataset = fromModel(m);
        List<Resource> results = dataset.listSubjectsWithProperty(m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex2")).toList();
        assertEquals(1, results.size());
        assert(results.contains(m.createResource("http://www.example.com/ex1")));
    }

    @Test
    public void testListObjectsOfProperty() {
        Model m = ModelFactory.createDefaultModel();
        m.add(m.createStatement(m.createResource("http://www.example.com/ex1"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex2"), m.createProperty("http://www.example.com/p2"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex4"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex5")));
        SPARQLDataset dataset = fromModel(m);
        List<RDFNode> results = dataset.listObjectsOfProperty(m.createResource("http://www.example.com/ex2"), m.createProperty("http://www.example.com/p2")).toList();
        assertEquals(1, results.size());
        assert(results.contains(m.createResource("http://www.example.com/ex2")));
    }

    @Test
    public void testListStatements() {
        Model m = ModelFactory.createDefaultModel();
        for(int i = 0; i < 100; i++) {
            m.add(m.createStatement(m.createResource("http://www.example.com/ex" + i), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex" + (i + 1))));
        }
        SPARQLDataset dataset = fromModel(m);
        List<Statement> results = dataset.listStatements().toList();
        assertEquals(100, results.size());
    }

    @Test
    public void testListStatements2() {
        Model m = ModelFactory.createDefaultModel();
        m.add(m.createStatement(m.createResource("http://www.example.com/ex1"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex2"), m.createProperty("http://www.example.com/p2"), m.createResource("http://www.example.com/ex2")));
        m.add(m.createStatement(m.createResource("http://www.example.com/ex4"), m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex5")));
        SPARQLDataset dataset = fromModel(m);
        {
        List<Resource> results = dataset.listStatements(null, null, null).toList().stream().map(s -> s.getSubject()).collect(Collectors.toList());
        assertEquals(3, results.size());
        assert(results.contains(m.createResource("http://www.example.com/ex1")));
        assert(results.contains(m.createResource("http://www.example.com/ex2")));
        assert(results.contains(m.createResource("http://www.example.com/ex4")));
        }
        {
        List<Resource> results = dataset.listStatements(null, m.createProperty("http://www.example.com/p1"), null).toList().stream().map(s -> s.getSubject()).collect(Collectors.toList());
        assertEquals(2, results.size());
        assert(results.contains(m.createResource("http://www.example.com/ex1")));
        assert(results.contains(m.createResource("http://www.example.com/ex4")));
        }
        {
        List<Resource> results = dataset.listStatements(null, m.createProperty("http://www.example.com/p1"), m.createResource("http://www.example.com/ex2")).toList().stream().map(s -> s.getSubject()).collect(Collectors.toList());
        assertEquals(1, results.size());
        assert(results.contains(m.createResource("http://www.example.com/ex1")));
        }
        {
        List<RDFNode> results = dataset.listStatements(m.createResource("http://www.example.com/ex2"), m.createProperty("http://www.example.com/p2"), null).toList().stream().map(s -> s.getSubject()).collect(Collectors.toList());
        assertEquals(1, results.size());
        assert(results.contains(m.createResource("http://www.example.com/ex2")));
        }

    }
}