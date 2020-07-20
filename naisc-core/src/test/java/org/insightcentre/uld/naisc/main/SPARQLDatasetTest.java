package org.insightcentre.uld.naisc.main;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.update.UpdateRequest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SPARQLDatasetTest {
    private SPARQLDataset fromModel(Model model) {
        return new SPARQLDataset("file:example", "test") {
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
        m.add(m.createStatement(m.createResource("file:ex1"), m.createProperty("file:p1"), m.createResource("file:ex2")));
        m.add(m.createStatement(m.createResource("file:ex2"), m.createProperty("file:p1"), m.createResource("file:ex2")));
        m.add(m.createStatement(m.createResource("file:ex4"), m.createProperty("file:p1"), m.createResource("file:ex5")));
        SPARQLDataset dataset = fromModel(m);
        List<Resource> results = dataset.listSubjects().toList();
        assertEquals(3, results.size());
        assert(results.contains(m.createResource("file:ex1")));
        assert(results.contains(m.createResource("file:ex2")));
        assert(results.contains(m.createResource("file:ex4")));
    }
}