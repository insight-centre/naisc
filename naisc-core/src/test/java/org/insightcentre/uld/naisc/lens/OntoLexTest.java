package org.insightcentre.uld.naisc.lens;

import eu.monnetproject.lang.Language;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.LensResult;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class OntoLexTest {

    public OntoLexTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static String testDoc = ""
            + "@prefix ontolex: <http://www.w3.org/ns/lemon/ontolex#> ."
            + "@prefix : <http://www.example.com/#> . "
            + ":foo_entry a ontolex:LexicalEntry ;"
            + "  ontolex:canonicalForm [ ontolex:writtenRep \"foo\"@en ] ;"
            + "  ontolex:sense [ ontolex:reference :foo ] ."
            + ""
            + ":bar_entry a ontolex:LexicalEntry ;"
            + "  ontolex:canonicalForm [ ontolex:writtenRep \"bar\"@en ] ;"
            + "  ontolex:sense [ ontolex:reference :bar ] .";
    
    /**
     * Test of makeLens method, of class OntoLex.
     */
    @Test
    public void testMakeLens() {
        System.out.println("makeLens");
        String tag = "none";
        Model sparqlData = ModelFactory.createDefaultModel();
        sparqlData.read(new StringReader(testDoc), "file:test#", "TURTLE");
        Map<String, Object> params = new HashMap<>();
        OntoLex instance = new OntoLex();
        Lens lens = instance.makeLens(new ModelDataset(sparqlData,"sparql"), params);
        Collection<LensResult> result = lens.extract(new URIRes("http://www.example.com/#foo", "sparql"), new URIRes("http://www.example.com/#bar", "sparql"));
        Option<LensResult> expResult = new Some<>(new LensResult(Language.ENGLISH, Language.ENGLISH, "foo", "bar", "ontolex"));
        assertEquals(expResult, result);
    }

}