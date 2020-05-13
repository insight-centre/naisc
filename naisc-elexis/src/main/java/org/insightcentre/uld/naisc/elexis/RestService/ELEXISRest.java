package org.insightcentre.uld.naisc.elexis.RestService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.Command;
import org.apache.jena.shared.Lock;
import org.apache.jena.shared.PrefixMapping;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

/**
 * Class to access the data from ELEXIS REST APIs
 * defined at: https://elexis-eu.github.io/elexis-rest/
 *
 * @author Suruchi Gupta
 */
public class ELEXISRest {
    private static URL endpoint;
    APIConnection apiConnection;

    /**
     * Creating a new object
     *
     * @param endpoint
     */
    public ELEXISRest(URL endpoint) {
        ELEXISRest.endpoint = endpoint;
        apiConnection = new APIConnection(endpoint);
    }

    /**
     * Calls dictionaries endpoint and returns list of available dictionaries
     *
     * @return List of all dictionaries available
     */
    public List<String> getDictionaries() throws MalformedURLException, JSONException {
        URL dictEndpoint = new URL(endpoint.toString()+"/dictionaries");
        String response = apiConnection.executeAPICall(dictEndpoint);

        JSONObject jsonResponse = new JSONObject(response);

        ArrayList<String> dictionaries = new ArrayList<String>();
        JSONArray dictArray = new JSONArray();
        if(jsonResponse.has("dictionaries")) {
            dictArray = (JSONArray) jsonResponse.get("dictionaries");
            //dictArray.forEach(dict -> dictionaries.add((String) dict));
            dictionaries.add(dictArray.toString());
        }
        return dictionaries;
    }

    /**
     * Fetches the elexis.rest.service.MetaData of the provided dictionary
     *
     * @param dictionary
     * @return elexis.rest.service.MetaData
     */
    public MetaData aboutDictionary(String dictionary) throws MalformedURLException, JsonProcessingException {
        URL aboutDictEndPoint = new URL(endpoint.toString()+"/about/"+dictionary);
        String response = apiConnection.executeAPICall(aboutDictEndPoint);

        ObjectMapper objectMapper = new ObjectMapper();
        MetaData metaData = objectMapper.readValue(response, MetaData.class);

        return metaData;
    }

    /**
     * Returns all the lemmas in the given dictionary
     *
     * @param dictionary
     * @return List of Lemmas
     * @throws MalformedURLException
     */
    public Lemma[] getAllLemmas(String dictionary) throws MalformedURLException, JsonProcessingException {
        URL allLemmasEndPoint = new URL(endpoint.toString()+"/list/"+dictionary);
        String response = apiConnection.executeAPICall(allLemmasEndPoint);

        ObjectMapper objectMapper = new ObjectMapper();
        Lemma[] allLemmas = objectMapper.readValue(response, Lemma[].class);

        return allLemmas;
    }

    /**
     * Returns list of entries for the given headword in a dictionary
     *
     * @param dictionary
     * @param headword
     * @return List of entries under the given headword
     * @throws JsonProcessingException
     * @throws MalformedURLException
     */
    public Lemma[] getHeadWordLookup(String dictionary, String headword) throws JsonProcessingException, MalformedURLException {
        URL headWordsEndPoint = new URL(endpoint.toString()+"/lemma/"+dictionary+"/"+headword);
        String response = apiConnection.executeAPICall(headWordsEndPoint);

        ObjectMapper objectMapper = new ObjectMapper();
        Lemma[] allHeadWords = objectMapper.readValue(response, Lemma[].class);

        return allHeadWords;
    }

    /**
     * Returns the entry in the dictionary in form of JSONObject
     *
     * @param dictionary
     * @param id
     * @return dictionary entry As JSON
     * @throws MalformedURLException
     */
    public JSONObject getEntryAsJSON(String dictionary, String id) throws MalformedURLException, JSONException {
        URL entryAsJSONEndPoint = new URL(endpoint.toString()+"/json/"+dictionary+"/"+id);
        String response = apiConnection.executeAPICall(entryAsJSONEndPoint);

        JSONObject entryAsJSON = new JSONObject(response);
        return entryAsJSON;
    }

    /**
     * Returns the entry in the dictionary in form of RDF
     *
     * @param dictionary
     * @param id
     * @return dictionary entry as RDF
     * @throws MalformedURLException
     */
    public Model getEntryAsTurtle(String dictionary, String id) throws MalformedURLException {
        URL entryAsJSONEndPoint = new URL(endpoint.toString()+"/ontolex/"+dictionary+"/"+id);
        String response = apiConnection.executeAPICall(entryAsJSONEndPoint);

        Model entryAsTurtle = new Model() {
            @Override
            public long size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public ResIterator listSubjects() {
                return null;
            }

            @Override
            public NsIterator listNameSpaces() {
                return null;
            }

            @Override
            public Resource getResource(String uri) {
                return null;
            }

            @Override
            public Property getProperty(String nameSpace, String localName) {
                return null;
            }

            @Override
            public Resource createResource() {
                return null;
            }

            @Override
            public Resource createResource(AnonId id) {
                return null;
            }

            @Override
            public Resource createResource(String uri) {
                return null;
            }

            @Override
            public Property createProperty(String nameSpace, String localName) {
                return null;
            }

            @Override
            public Literal createLiteral(String v, String language) {
                return null;
            }

            @Override
            public Literal createLiteral(String v, boolean wellFormed) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(String lex, RDFDatatype dtype) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(Object value, RDFDatatype dtype) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(Object value) {
                return null;
            }

            @Override
            public Statement createStatement(Resource s, Property p, RDFNode o) {
                return null;
            }

            @Override
            public RDFList createList() {
                return null;
            }

            @Override
            public RDFList createList(Iterator<? extends RDFNode> members) {
                return null;
            }

            @Override
            public RDFList createList(RDFNode... members) {
                return null;
            }

            @Override
            public Model add(Statement s) {
                return null;
            }

            @Override
            public Model add(Statement[] statements) {
                return null;
            }

            @Override
            public Model remove(Statement[] statements) {
                return null;
            }

            @Override
            public Model add(List<Statement> statements) {
                return null;
            }

            @Override
            public Model remove(List<Statement> statements) {
                return null;
            }

            @Override
            public Model add(StmtIterator iter) {
                return null;
            }

            @Override
            public Model add(Model m) {
                return null;
            }

            @Override
            public Model read(String url) {
                return null;
            }

            @Override
            public Model read(InputStream in, String base) {
                return null;
            }

            @Override
            public Model read(InputStream in, String base, String lang) {
                return null;
            }

            @Override
            public Model read(Reader reader, String base) {
                return null;
            }

            @Override
            public Model read(String url, String lang) {
                return null;
            }

            @Override
            public Model read(Reader reader, String base, String lang) {
                return null;
            }

            @Override
            public Model read(String url, String base, String lang) {
                return null;
            }

            @Override
            public Model write(Writer writer) {
                return null;
            }

            @Override
            public Model write(Writer writer, String lang) {
                return null;
            }

            @Override
            public Model write(Writer writer, String lang, String base) {
                return null;
            }

            @Override
            public Model write(OutputStream out) {
                return null;
            }

            @Override
            public Model write(OutputStream out, String lang) {
                return null;
            }

            @Override
            public Model write(OutputStream out, String lang, String base) {
                return null;
            }

            @Override
            public Model remove(Statement s) {
                return null;
            }

            @Override
            public Statement getRequiredProperty(Resource s, Property p) {
                return null;
            }

            @Override
            public Statement getRequiredProperty(Resource s, Property p, String lang) {
                return null;
            }

            @Override
            public Statement getProperty(Resource s, Property p) {
                return null;
            }

            @Override
            public Statement getProperty(Resource s, Property p, String lang) {
                return null;
            }

            @Override
            public ResIterator listSubjectsWithProperty(Property p) {
                return null;
            }

            @Override
            public ResIterator listResourcesWithProperty(Property p) {
                return null;
            }

            @Override
            public ResIterator listSubjectsWithProperty(Property p, RDFNode o) {
                return null;
            }

            @Override
            public ResIterator listResourcesWithProperty(Property p, RDFNode o) {
                return null;
            }

            @Override
            public NodeIterator listObjects() {
                return null;
            }

            @Override
            public NodeIterator listObjectsOfProperty(Property p) {
                return null;
            }

            @Override
            public NodeIterator listObjectsOfProperty(Resource s, Property p) {
                return null;
            }

            @Override
            public boolean contains(Resource s, Property p) {
                return false;
            }

            @Override
            public boolean containsResource(RDFNode r) {
                return false;
            }

            @Override
            public boolean contains(Resource s, Property p, RDFNode o) {
                return false;
            }

            @Override
            public boolean contains(Statement s) {
                return false;
            }

            @Override
            public boolean containsAny(StmtIterator iter) {
                return false;
            }

            @Override
            public boolean containsAll(StmtIterator iter) {
                return false;
            }

            @Override
            public boolean containsAny(Model model) {
                return false;
            }

            @Override
            public boolean containsAll(Model model) {
                return false;
            }

            @Override
            public boolean isReified(Statement s) {
                return false;
            }

            @Override
            public Resource getAnyReifiedStatement(Statement s) {
                return null;
            }

            @Override
            public void removeAllReifications(Statement s) {

            }

            @Override
            public void removeReification(ReifiedStatement rs) {

            }

            @Override
            public StmtIterator listStatements() {
                return null;
            }

            @Override
            public StmtIterator listStatements(Selector s) {
                return null;
            }

            @Override
            public StmtIterator listStatements(Resource s, Property p, RDFNode o) {
                return null;
            }

            @Override
            public ReifiedStatement createReifiedStatement(Statement s) {
                return null;
            }

            @Override
            public ReifiedStatement createReifiedStatement(String uri, Statement s) {
                return null;
            }

            @Override
            public RSIterator listReifiedStatements() {
                return null;
            }

            @Override
            public RSIterator listReifiedStatements(Statement st) {
                return null;
            }

            @Override
            public Model query(Selector s) {
                return null;
            }

            @Override
            public Model union(Model model) {
                return null;
            }

            @Override
            public Model intersection(Model model) {
                return null;
            }

            @Override
            public Model difference(Model model) {
                return null;
            }

            @Override
            public Model begin() {
                return null;
            }

            @Override
            public Model abort() {
                return null;
            }

            @Override
            public Model commit() {
                return null;
            }

            @Override
            public Object executeInTransaction(Command cmd) {
                return null;
            }

            @Override
            public void executeInTxn(Runnable action) {

            }

            @Override
            public <T> T calculateInTxn(Supplier<T> action) {
                return null;
            }

            @Override
            public boolean independent() {
                return false;
            }

            @Override
            public boolean supportsTransactions() {
                return false;
            }

            @Override
            public boolean supportsSetOperations() {
                return false;
            }

            @Override
            public boolean isIsomorphicWith(Model g) {
                return false;
            }

            @Override
            public void close() {

            }

            @Override
            public Lock getLock() {
                return null;
            }

            @Override
            public Model register(ModelChangedListener listener) {
                return null;
            }

            @Override
            public Model unregister(ModelChangedListener listener) {
                return null;
            }

            @Override
            public Model notifyEvent(Object e) {
                return null;
            }

            @Override
            public Model removeAll() {
                return null;
            }

            @Override
            public Model removeAll(Resource s, Property p, RDFNode r) {
                return null;
            }

            @Override
            public boolean isClosed() {
                return false;
            }

            @Override
            public Model setNsPrefix(String prefix, String uri) {
                return null;
            }

            @Override
            public Model removeNsPrefix(String prefix) {
                return null;
            }

            @Override
            public Model clearNsPrefixMap() {
                return null;
            }

            @Override
            public Model setNsPrefixes(PrefixMapping other) {
                return null;
            }

            @Override
            public Model setNsPrefixes(Map<String, String> map) {
                return null;
            }

            @Override
            public Model withDefaultMappings(PrefixMapping map) {
                return null;
            }

            @Override
            public Resource getResource(String uri, ResourceF f) {
                return null;
            }

            @Override
            public Property getProperty(String uri) {
                return null;
            }

            @Override
            public Bag getBag(String uri) {
                return null;
            }

            @Override
            public Bag getBag(Resource r) {
                return null;
            }

            @Override
            public Alt getAlt(String uri) {
                return null;
            }

            @Override
            public Alt getAlt(Resource r) {
                return null;
            }

            @Override
            public Seq getSeq(String uri) {
                return null;
            }

            @Override
            public Seq getSeq(Resource r) {
                return null;
            }

            @Override
            public RDFList getList(String uri) {
                return null;
            }

            @Override
            public RDFList getList(Resource r) {
                return null;
            }

            @Override
            public Resource createResource(Resource type) {
                return null;
            }

            @Override
            public RDFNode getRDFNode(Node n) {
                return null;
            }

            @Override
            public Resource createResource(String uri, Resource type) {
                return null;
            }

            @Override
            public Resource createResource(ResourceF f) {
                return null;
            }

            @Override
            public Resource createResource(String uri, ResourceF f) {
                return null;
            }

            @Override
            public Property createProperty(String uri) {
                return null;
            }

            @Override
            public Literal createLiteral(String v) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(boolean v) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(int v) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(long v) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(Calendar d) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(char v) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(float v) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(double v) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(String v) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(String lex, String typeURI) {
                return null;
            }

            @Override
            public Literal createTypedLiteral(Object value, String typeURI) {
                return null;
            }

            @Override
            public Statement createLiteralStatement(Resource s, Property p, boolean o) {
                return null;
            }

            @Override
            public Statement createLiteralStatement(Resource s, Property p, float o) {
                return null;
            }

            @Override
            public Statement createLiteralStatement(Resource s, Property p, double o) {
                return null;
            }

            @Override
            public Statement createLiteralStatement(Resource s, Property p, long o) {
                return null;
            }

            @Override
            public Statement createLiteralStatement(Resource s, Property p, int o) {
                return null;
            }

            @Override
            public Statement createLiteralStatement(Resource s, Property p, char o) {
                return null;
            }

            @Override
            public Statement createLiteralStatement(Resource s, Property p, Object o) {
                return null;
            }

            @Override
            public Statement createStatement(Resource s, Property p, String o) {
                return null;
            }

            @Override
            public Statement createStatement(Resource s, Property p, String o, String l) {
                return null;
            }

            @Override
            public Statement createStatement(Resource s, Property p, String o, boolean wellFormed) {
                return null;
            }

            @Override
            public Statement createStatement(Resource s, Property p, String o, String l, boolean wellFormed) {
                return null;
            }

            @Override
            public Bag createBag() {
                return null;
            }

            @Override
            public Bag createBag(String uri) {
                return null;
            }

            @Override
            public Alt createAlt() {
                return null;
            }

            @Override
            public Alt createAlt(String uri) {
                return null;
            }

            @Override
            public Seq createSeq() {
                return null;
            }

            @Override
            public Seq createSeq(String uri) {
                return null;
            }

            @Override
            public Model add(Resource s, Property p, RDFNode o) {
                return null;
            }

            @Override
            public Model addLiteral(Resource s, Property p, boolean o) {
                return null;
            }

            @Override
            public Model addLiteral(Resource s, Property p, long o) {
                return null;
            }

            @Override
            public Model addLiteral(Resource s, Property p, int o) {
                return null;
            }

            @Override
            public Model addLiteral(Resource s, Property p, char o) {
                return null;
            }

            @Override
            public Model addLiteral(Resource s, Property p, float o) {
                return null;
            }

            @Override
            public Model addLiteral(Resource s, Property p, double o) {
                return null;
            }

            @Override
            public Model addLiteral(Resource s, Property p, Object o) {
                return null;
            }

            @Override
            public Model addLiteral(Resource s, Property p, Literal o) {
                return null;
            }

            @Override
            public Model add(Resource s, Property p, String o) {
                return null;
            }

            @Override
            public Model add(Resource s, Property p, String lex, RDFDatatype datatype) {
                return null;
            }

            @Override
            public Model add(Resource s, Property p, String o, boolean wellFormed) {
                return null;
            }

            @Override
            public Model add(Resource s, Property p, String o, String l) {
                return null;
            }

            @Override
            public Model remove(Resource s, Property p, RDFNode o) {
                return null;
            }

            @Override
            public Model remove(StmtIterator iter) {
                return null;
            }

            @Override
            public Model remove(Model m) {
                return null;
            }

            @Override
            public StmtIterator listLiteralStatements(Resource subject, Property predicate, boolean object) {
                return null;
            }

            @Override
            public StmtIterator listLiteralStatements(Resource subject, Property predicate, char object) {
                return null;
            }

            @Override
            public StmtIterator listLiteralStatements(Resource subject, Property predicate, long object) {
                return null;
            }

            @Override
            public StmtIterator listLiteralStatements(Resource subject, Property predicate, int object) {
                return null;
            }

            @Override
            public StmtIterator listLiteralStatements(Resource subject, Property predicate, float object) {
                return null;
            }

            @Override
            public StmtIterator listLiteralStatements(Resource subject, Property predicate, double object) {
                return null;
            }

            @Override
            public StmtIterator listStatements(Resource subject, Property predicate, String object) {
                return null;
            }

            @Override
            public StmtIterator listStatements(Resource subject, Property predicate, String object, String lang) {
                return null;
            }

            @Override
            public ResIterator listResourcesWithProperty(Property p, boolean o) {
                return null;
            }

            @Override
            public ResIterator listResourcesWithProperty(Property p, long o) {
                return null;
            }

            @Override
            public ResIterator listResourcesWithProperty(Property p, char o) {
                return null;
            }

            @Override
            public ResIterator listResourcesWithProperty(Property p, float o) {
                return null;
            }

            @Override
            public ResIterator listResourcesWithProperty(Property p, double o) {
                return null;
            }

            @Override
            public ResIterator listResourcesWithProperty(Property p, Object o) {
                return null;
            }

            @Override
            public ResIterator listSubjectsWithProperty(Property p, String o) {
                return null;
            }

            @Override
            public ResIterator listSubjectsWithProperty(Property p, String o, String l) {
                return null;
            }

            @Override
            public boolean containsLiteral(Resource s, Property p, boolean o) {
                return false;
            }

            @Override
            public boolean containsLiteral(Resource s, Property p, long o) {
                return false;
            }

            @Override
            public boolean containsLiteral(Resource s, Property p, int o) {
                return false;
            }

            @Override
            public boolean containsLiteral(Resource s, Property p, char o) {
                return false;
            }

            @Override
            public boolean containsLiteral(Resource s, Property p, float o) {
                return false;
            }

            @Override
            public boolean containsLiteral(Resource s, Property p, double o) {
                return false;
            }

            @Override
            public boolean containsLiteral(Resource s, Property p, Object o) {
                return false;
            }

            @Override
            public boolean contains(Resource s, Property p, String o) {
                return false;
            }

            @Override
            public boolean contains(Resource s, Property p, String o, String l) {
                return false;
            }

            @Override
            public Statement asStatement(Triple t) {
                return null;
            }

            @Override
            public Graph getGraph() {
                return null;
            }

            @Override
            public RDFNode asRDFNode(Node n) {
                return null;
            }

            @Override
            public Resource wrapAsResource(Node n) {
                return null;
            }

            @Override
            public RDFReader getReader() {
                return null;
            }

            @Override
            public RDFReader getReader(String lang) {
                return null;
            }

            @Override
            public String setReaderClassName(String lang, String className) {
                return null;
            }

            @Override
            public void resetRDFReaderF() {

            }

            @Override
            public String removeReader(String lang) throws IllegalArgumentException {
                return null;
            }

            @Override
            public RDFWriter getWriter() {
                return null;
            }

            @Override
            public RDFWriter getWriter(String lang) {
                return null;
            }

            @Override
            public String setWriterClassName(String lang, String className) {
                return null;
            }

            @Override
            public void resetRDFWriterF() {

            }

            @Override
            public String removeWriter(String lang) throws IllegalArgumentException {
                return null;
            }

            @Override
            public void enterCriticalSection(boolean readLockRequested) {

            }

            @Override
            public void leaveCriticalSection() {

            }

            @Override
            public String getNsPrefixURI(String prefix) {
                return null;
            }

            @Override
            public String getNsURIPrefix(String uri) {
                return null;
            }

            @Override
            public Map<String, String> getNsPrefixMap() {
                return null;
            }

            @Override
            public String expandPrefix(String prefixed) {
                return null;
            }

            @Override
            public String shortForm(String uri) {
                return null;
            }

            @Override
            public String qnameFor(String uri) {
                return null;
            }

            @Override
            public PrefixMapping lock() {
                return null;
            }

            @Override
            public int numPrefixes() {
                return 0;
            }

            @Override
            public boolean samePrefixMappingAs(PrefixMapping other) {
                return false;
            }
        };
        entryAsTurtle.read(new ByteArrayInputStream(response.getBytes()), null, "TTL");

        return entryAsTurtle;
    }
}