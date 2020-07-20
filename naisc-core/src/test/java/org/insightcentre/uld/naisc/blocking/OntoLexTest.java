package org.insightcentre.uld.naisc.blocking;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Blocking;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;
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

    private final String ONTOLEX_DOC = ""
            + "@prefix ontolex: <http://www.w3.org/ns/lemon/ontolex#> ."
            + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> ."
            + ""
            + "<#cat> a ontolex:LexicalEntry ;"
            + "  ontolex:canonicalForm [ "
            + "    ontolex:writtenRep \"cat\"@en "
            + "  ] ;"
            + "  ontolex:sense <#cat-1>, <#cat-2>, <#cat-3> ."
            + " <#cat-1> skos:definition \"A small domesticated carnivorous mammal\"@en ."
            + " <#cat-2> skos:definition \"short for cat-o'-nine-tails\"@en ."
            + " <#cat-3> skos:definition \"A man (jazz)\"@en ."
            + ""
            + "<#bank> a ontolex:LexicalEntry ; "
            + "  ontolex:canonicalForm ["
            + "   ontolex:writtenRep \"bank\"@en "
            + "  ] ;"
            + "  ontolex:sense <#bank-1>, <#bank-2> . "
            + " <#bank-1> skos:definition \"The land alongside or sloping down to a river or lake.\"@en . "
            + " <#bank-2> skos:definition \"A financial establishment\"@en . "
            + ""
            + "<#herring> a ontolex:LexicalEntry ;"
            + "  ontolex:canonicalForm ["
            + "   ontolex:writtenRep \"herring\"@en "
            + "  ] ;"
            + "  ontolex:sense <#herring-1> ."
            + " <#herring-1> skos:definition \"A clue or piece of information which is or is intended to be misleading or distracting.\"@en .";
    
    private final String SIMPLE_DOC = ""
            + "<file:DDO/11001660> <http://www.w3.org/2000/01/rdf-schema#label> \"cat\"@en .\n" +
"<file:DDO/11001660> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.lexinfo.net/ontology/2.0/lexinfo#Noun> .\n" +
"<file:DDO/11001660> <http://www.w3.org/ns/lemon/ontolex#sense> <file:DDO/11001660/sense/21002471>, <file:DDO/11001660/sense/21002472>, <file:DDO/11001660/sense/21002485> . \n" +
"<file:DDO/11001660/sense/21002471> <http://www.w3.org/2004/02/skos/core#definition> \"opdigtet nyhed der bringes i avis, radio, tv e.l.\"@da. \n" +
"<file:DDO/11001660/sense/21002472> <http://www.w3.org/2004/02/skos/core#definition> \"denne fugl brugt som madvare\"@da.\n" +
"<file:DDO/11001660/sense/21002485> <http://www.w3.org/2004/02/skos/core#definition> \"fugl af ordenen Anseriformes\"@da ."
            + "<file:DDO/11001660a> <http://www.w3.org/2000/01/rdf-schema#label> \"herring\"@da .\n" +
"<file:DDO/11001660a> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.lexinfo.net/ontology/2.0/lexinfo#Noun> .\n" +
"<file:DDO/11001660a> <http://www.w3.org/ns/lemon/ontolex#sense> <file:DDO/11001660/sense/21002471a>, <file:DDO/11001660/sense/21002472a>, <file:DDO/11001660/sense/21002485a> . \n" +
"<file:DDO/11001660/sense/21002471a> <http://www.w3.org/2004/02/skos/core#definition> \"opdigtet nyhed der bringes i avis, radio, tv e.l.\"@da. \n" +
"<file:DDO/11001660/sense/21002472a> <http://www.w3.org/2004/02/skos/core#definition> \"denne fugl brugt som madvare\"@da.\n" +
"<file:DDO/11001660/sense/21002485a> <http://www.w3.org/2004/02/skos/core#definition> \"fugl af ordenen Anseriformes\"@da ."
            + "<file:DDO/11001660n> <http://www.w3.org/2000/01/rdf-schema#label> \"bank\"@da .\n" +
"<file:DDO/11001660n> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.lexinfo.net/ontology/2.0/lexinfo#Noun> .\n" +
"<file:DDO/11001660n> <http://www.w3.org/ns/lemon/ontolex#sense> <file:DDO/11001660/sense/21002471n>, <file:DDO/11001660/sense/21002472n>, <file:DDO/11001660/sense/21002485n> . \n" +
"<file:DDO/11001660/sense/21002471n> <http://www.w3.org/2004/02/skos/core#definition> \"opdigtet nyhed der bringes i avis, radio, tv e.l.\"@da. \n" +
"<file:DDO/11001660/sense/21002472n> <http://www.w3.org/2004/02/skos/core#definition> \"denne fugl brugt som madvare\"@da.\n" +
"<file:DDO/11001660/sense/21002485n> <http://www.w3.org/2004/02/skos/core#definition> \"fugl af ordenen Anseriformes\"@da .";
    
    /**
     * Test of makeBlockingStrategy method, of class OntoLex.
     */
    @Test
    public void testMakeBlockingStrategy() {
        System.out.println("makeBlockingStrategy");
        Map<String, Object> params = new HashMap<>();
        OntoLex instance = new OntoLex();
        BlockingStrategy blocker = instance.makeBlockingStrategy(params, Lazy.fromClosure(() -> null), ExecuteListeners.NONE);
        Model left = ModelFactory.createDefaultModel();
        left.read(new StringReader(ONTOLEX_DOC), "http://www.example.com/", "TURTLE");
        Model right = ModelFactory.createDefaultModel();
        right.read(new StringReader(SIMPLE_DOC), "http://www.example.com/", "TURTLE");
        assertEquals(9, blocker.estimateSize(new ModelDataset(left,"left"), new ModelDataset(right,"right")));
        Set<String> leftMatching = new HashSet<>(Arrays.asList("http://www.example.com/#cat-1","http://www.example.com/#cat-2","http://www.example.com/#cat-3"));
        Set<String> rightMatching = new HashSet<>(Arrays.asList("file:DDO/11001660/sense/21002471","file:DDO/11001660/sense/21002472","file:DDO/11001660/sense/21002485"));
        HashSet<Blocking> s = new HashSet<>();
        for(Blocking rr : blocker.block(new ModelDataset(left,"left"), new ModelDataset(right,"right"))) {
            assert(leftMatching.contains(rr.entity1.uri));
            assert(rightMatching.contains(rr.entity2.uri));
            s.add(rr);
        }
        assertEquals(9, s.size());
    }

}