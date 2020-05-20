package org.insightcentre.uld.naisc.lens;

import eu.monnetproject.lang.Language;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.LensResult;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
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
    public void testMakeLens() throws Exception {
        if(System.getProperty("command.test") != null) {
        try {
            System.out.println("makeLens");
            Model model = ModelFactory.createDefaultModel();
            String tag = "command";
            Dataset dataset = new DefaultDatasetLoader.EndpointDataset(new URL("http://www.example.com"), "model");
            Map<String, Object> params = new HashMap<>();
            params.put("command", "python3 src/test/resources/test-lens.py");
            params.put("id", "test");
            Command instance = new Command();
            Lens lens = instance.makeLens(dataset, params);
            Collection<LensResult> result = lens.extract(
                    new URIRes("http://www.example.com/foo", "left"),
                    new URIRes("http://www.example.com/bar", "right"));
            Option<LensResult> expResult = new Some<>(
                    new LensResult(Language.UNDEFINED, Language.UNDEFINED, "http://www.example.com/foo", "http://www.example.com/bar", null));
            assertEquals(expResult, result);
        } catch (ExternalCommandException x) {
            x.printStackTrace();
        }
        }
    }

}
