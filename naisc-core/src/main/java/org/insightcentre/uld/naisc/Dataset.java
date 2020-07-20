package org.insightcentre.uld.naisc;

import java.net.URL;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.util.Option;

/**
 * Get a dataset either as a model or if available as a SPARQL endpoint URL
 * 
 * @author John McCrae
 */
public interface Dataset {
    /**
     * Return this dataset as a Jena model
     * @return The Jena Model (if available)
     */
    //Option<Model> asModel();
    /**
     * Return this dataset as a SPARQL endpoint
     * @return The endpoint URL (if available)
     */
    Option<URL> asEndpoint();

    public ResIterator listSubjects();

    public Property createProperty(String uri);
    public Resource createResource(String uri);

    public ResIterator listSubjectsWithProperty(Property createProperty);
    public ResIterator listSubjectsWithProperty(Property createProperty, RDFNode object);

    public NodeIterator listObjectsOfProperty(Resource r, Property createProperty);

    public StmtIterator listStatements(Resource source, Property prop, RDFNode rdfNode);

    public StmtIterator listStatements();
    
    public QueryExecution createQuery(Query query);

    public String id();
}
