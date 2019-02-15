package org.insightcentre.uld.naisc.lens;

import eu.monnetproject.lang.Language;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.util.ExternalCommandException;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author John McCrae
 */
public class CommandTest {

    public CommandTest() {
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

    /**
     * Test of makeLens method, of class Command.
     */
    @Test
    public void testMakeLens() {
        try {
            System.out.println("makeLens");
            Model model = ModelFactory.createDefaultModel();
            String tag = "command";
            Dataset dataset = new Dataset() {
                @Override
                public Option<Model> asModel() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Option<URL> asEndpoint() {
                    try {
                        return new Some<>(new URL("http://www.example.com/"));
                    } catch (MalformedURLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
            Map<String, Object> params = new HashMap<>();
            params.put("command", "python3 src/test/resources/test-lens.py");
            params.put("id", "test");
            Command instance = new Command();
            Lens lens = instance.makeLens(tag, dataset, params);
            Option<LangStringPair> result = lens.extract(
                    model.createResource("http://www.example.com/foo"),
                    model.createResource("http://www.example.com/bar"));
            Option<LangStringPair> expResult = new Some<>(
                    new LangStringPair(Language.UNDEFINED, Language.UNDEFINED, "http://www.example.com/foo", "http://www.example.com/bar"));
            assertEquals(expResult, result);
        } catch (ExternalCommandException x) {
            x.printStackTrace();
        }
    }

}
