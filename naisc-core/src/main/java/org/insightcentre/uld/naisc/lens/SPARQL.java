package org.insightcentre.uld.naisc.lens;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import eu.monnetproject.lang.LanguageCodeFormatException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.LensFactory;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * A lens that is implemented by a SPARQL query. The query should return exactly
 * two string literals and should contain the special variables $entity1 and
 * $entity2. For example<br>
 *
 * <code>
 * PREFIX rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;<br>
 * SELECT ?label1 ?label2 WHERE { <br>
 *   $entity1 rdfs:label ?label1 .<br> 
 *   $entity2 rdfs:label ?label2 .<br> 
 * }
 * </code>
 *
 * @author John McCrae
 */
public class SPARQL implements LensFactory {

    @Override
    public Lens makeLens(String tag, Model sparqlData, Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if(config.query == null) {
            throw new ConfigurationException("Query must be given for SPARQL lens");
        }
        return new SPARQLImpl(config.query, sparqlData, config.baseURI, tag);
    }

    /**
     * Configuration of the SPARQL lens.
     */
    public static class Configuration {
        /**
         * The SPARQL query.
         */
        @ConfigurationParameter(description = "The SPARQL query")
        public String query;
        
        /**
         * A base URI for the query (optional).
         */
        @ConfigurationParameter(description = "A base URI for the query (optional)")
        public String baseURI;
    }

    private static class SPARQLImpl implements Lens {

        private final String query;
        private final Model model;
        private final String baseURI;
        private final String tag;

        public SPARQLImpl(String query, Model model, String baseURI, String tag) {
            this.query = query;
            this.model = model;
            this.baseURI = baseURI;
            this.tag = tag;
        }


        @Override
        public String id() {
            return String.format("sparql-%04x", query.hashCode());
        }

        @Override
        public Option<LangStringPair> extract(Resource entity1, Resource entity2) {
            String queryString = this.query.replaceAll("\\$entity1", "<" + entity1.getURI() + ">")
                    .replaceAll("\\$entity2", "<" + entity2.getURI() + ">");
            Query sparqlQuery = baseURI == null ? QueryFactory.create(queryString)
                    : QueryFactory.create(queryString, baseURI);
            try (QueryExecution qexec = QueryExecutionFactory.create(sparqlQuery, model)) {
                ResultSet results = qexec.execSelect();
                for (; results.hasNext();) {
                    QuerySolution soln = results.nextSolution();
                    List<String> varNames = new ArrayList<>();
                    Iterator<String> i = soln.varNames();
                    while(i.hasNext())
                        varNames.add(i.next());
                    if(varNames.size() != 2) {
                        throw new RuntimeException("SPARQL query return more than one variable, this is not allowed for extracting labels");
                    }
                    RDFNode node1 = soln.get(varNames.get(0));
                    RDFNode node2 = soln.get(varNames.get(1));
                    if(node1 instanceof Literal && node2 instanceof Literal) {
                        Literal l1 = (Literal)node1;
                        Literal l2 = (Literal)node2;
                        return new Some<>(
                                new LangStringPair(
                                        toLang(l1.getLanguage()),
                                        toLang(l2.getLanguage()),
                                        l1.getLexicalForm(), l2.getLexicalForm()));
                    }
                }
            }
            return new None<>();
        }

        @Override
        public String tag() {
            return tag;
        }

    }
    
    private static Language toLang(String s) {
        if(s == null || s.equals("")) {
            return Language.UNDEFINED;
        } else {
            try {
                return Language.get(s);
            } catch(LanguageCodeFormatException x) {
                System.err.println("Bad language code in RDF: " + s);
                return Language.UNDEFINED;
            }
        }
    }
}
