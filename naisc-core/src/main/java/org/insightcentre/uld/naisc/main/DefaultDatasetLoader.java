package org.insightcentre.uld.naisc.main;

import java.io.*;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * The standard dataset loader
 *
 * @author John McCrae
 */
public class DefaultDatasetLoader implements DatasetLoader<ModelDataset> {

    @Override
    public ModelDataset fromFile(File file, String name) throws IOException {

        final Model model = ModelFactory.createDefaultModel();
        if(file.getName().endsWith(".rdf")) {
            model.read(new FileReader(file), file.toURI().toString(), "RDF/XML");
        } else if(file.getName().endsWith(".ttl")) {
            model.read(new FileReader(file), file.toURI().toString(), "Turtle");
        } else if(file.getName().endsWith(".nt")) {
            model.read(new FileReader(file), file.toURI().toString(), "N-TRIPLES");
        } else if(file.getName().endsWith(".rdf.gz")) {
            model.read(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))), file.toURI().toString(), "RDF/XML");
        } else if(file.getName().endsWith(".ttl.gz")) {
            model.read(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))), file.toURI().toString(), "Turtle");
        } else if(file.getName().endsWith(".nt.gz")) {
            model.read(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))), file.toURI().toString(), "N-TRIPLES");
        } else {
            model.read(new FileReader(file), file.toURI().toString(), "RDF/XML");
        }
        return new ModelDataset(model, name, file.toURI().toURL());
    }

    @Override
    public EndpointDataset fromEndpoint(final URL endpoint) {
        return new EndpointDataset(endpoint, endpoint.toString());
    }

    @Override
    public ModelDataset combine(ModelDataset dataset1, ModelDataset dataset2, String name) {
        final Model combined = ModelFactory.createDefaultModel();
        final Model leftModel = dataset1.model;
        final Model rightModel = dataset2.model;
        combined.add(leftModel);
        combined.add(rightModel);
        return new ModelDataset(combined, dataset1.id() + "+" + dataset2.id(), null);
    }

    public static class ModelDataset implements Dataset {
        public final Model model;
        public final String id;
        public final URL location;

        public ModelDataset(Model model, String id, URL location) {
            this.model = model;
            this.id = id;
            this.location = location;
        }

        @Override
        public URL getLocation() {
            return location;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Option<URL> asEndpoint() {
            return new None<>();
        }

        @Override
        public ResIterator listSubjects() {
            return model.listSubjects();
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
        public ResIterator listSubjectsWithProperty(Property createProperty) {
            return model.listSubjectsWithProperty(createProperty);
        }

        @Override
        public ResIterator listSubjectsWithProperty(Property createProperty, RDFNode object) {
            return model.listSubjectsWithProperty(createProperty, object);
        }

        @Override
        public NodeIterator listObjectsOfProperty(Resource r, Property createProperty) {
            return model.listObjectsOfProperty(r, createProperty);
        }

        @Override
        public StmtIterator listStatements(Resource source, Property prop, RDFNode rdfNode) {
            return model.listStatements(source, prop, rdfNode);
        }

        @Override
        public StmtIterator listStatements() {
            return model.listStatements();
        }

        @Override
        public QueryExecution createQuery(Query query) {
            return QueryExecutionFactory.create(query, model);
        }
        
    }
    
    public static class EndpointDataset implements Dataset {
        private final URL url;
        private final String id;

        public EndpointDataset(URL url, String id) {
            this.url = url;
            this.id = id;
        }

        @Override
        public URL getLocation() {
            return url;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Option<URL> asEndpoint() {
            return new Some<>(url);
        }

        @Override
        public ResIterator listSubjects() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Property createProperty(String uri) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Resource createResource(String uri) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ResIterator listSubjectsWithProperty(Property createProperty) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ResIterator listSubjectsWithProperty(Property createProperty, RDFNode object) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public NodeIterator listObjectsOfProperty(Resource r, Property createProperty) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public StmtIterator listStatements(Resource source, Property prop, RDFNode rdfNode) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public StmtIterator listStatements() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public QueryExecution createQuery(Query query) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}
