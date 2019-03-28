package org.insightcentre.uld.naisc.meas;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;

/**
 *
 * @author John McCrae
 */
public class SPARQLEndpointServlet extends HttpServlet {

    private static final Map<String, Model> models = new HashMap<>();

    public static void registerModel(String id, Model model) {
        models.put(id, model);
    }

    public static void deregisterModel(String id) {
        models.remove(id);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (models.containsKey(path)) {
            Model model = models.get(path);
            String queryString = req.getParameter("query");
            String format = req.getParameter("format");
            if(format == null) {
                format = SPARQL_XML_MIME;
                String accept = req.getHeader("Accept");
                if(accept != null) {
                    int sparqlIndex = accept.indexOf(SPARQL_XML_MIME);
                    int jsonIndex = accept.indexOf(SPARQL_JSON_MIME);
                    if(sparqlIndex < 0) sparqlIndex = Integer.MAX_VALUE;
                    if(jsonIndex < 0) jsonIndex = Integer.MAX_VALUE;
                    if(jsonIndex > sparqlIndex) {
                        format = SPARQL_JSON_MIME;
                    } 
                }
            }
            //String defaultGraphUri = req.getParameter("default-graph-uri");
            //String namedGraphUri = req.getParameter("named-graph-uri");
            if (queryString == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No query");
                return;
            }

            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet rs = qexec.execSelect();
                if(format.equals(SPARQL_XML_MIME)) {
                    resp.setContentType(SPARQL_XML_MIME);
                    try (PrintWriter out = resp.getWriter()) {
                        out.println(ResultSetFormatter.asXMLString(rs));
                    }
                } else if(format.equals(SPARQL_JSON_MIME)) {
                    resp.setContentType(SPARQL_JSON_MIME);
                    try (OutputStream out = resp.getOutputStream()) {
                        ResultSetFormatter.outputAsJSON(out, rs);
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Format was not understood: " + format);
                }
            }

        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    private static final String SPARQL_JSON_MIME = "application/sparql-results+json";
    private static final String SPARQL_XML_MIME =  "application/sparql-results+xml";

}
