package org.insightcentre.uld.naisc.scorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.ScorerTrainer;
import org.insightcentre.uld.naisc.util.ExternalCommandException;
import org.insightcentre.uld.naisc.util.Option;
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
     * Test of id method, of class Command.
     */
    @Test
    public void testId() {
        System.out.println("id");
        Command instance = new Command();
        String expResult = "command";
        String result = instance.id();
        assertEquals(expResult, result);
    }

    /**
     * Test of makeScorer method, of class Command.
     */
    @Test
    public void testMakeScorer() {
        if(System.getProperty("command.test") != null) {
        try {
            System.out.println("Command.makeScorer");
            Model model = ModelFactory.createDefaultModel();
            Map<String, Object> params = new HashMap<>();
            params.put("command", "python3 src/test/resources/test-scorer.py");
            Command instance = new Command();

            Scorer scorer = instance.makeScorer(params);
            double result = scorer.similarity(new FeatureSet(new String[]{"foo"}, "foo", new double[]{0.2},
                    model.createResource("http://www.example.com/uri1"), model.createResource("http://www.example.com/uri2")));
            double expResult = 0.2;
            assertEquals(expResult, result, 0.0);
        } catch (ExternalCommandException x) {
            x.printStackTrace();
        }
        }
    }

    /**
     * Test of makeTrainer method, of class Command.
     */
    //@Test
    public void testMakeTrainer() {
        try {
            System.out.println("makeTrainer");
            Map<String, Object> params = new HashMap<>();
            params.put("command", "python3 src/test/resources/test-scorer.py");
            params.put("trainCommand", "cat > /dev/null");
            Command instance = new Command();
            Option<ScorerTrainer> result = instance.makeTrainer(params);
            result.get().train(new ArrayList<>());
        } catch (ExternalCommandException x) {
            x.printStackTrace();
        }
    }

}