package org.insightcentre.uld.naisc.matcher;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.util.ExternalCommandException;
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
     * Test of makeMatcher method, of class Command.
     */
    @Test
    public void testMakeMatcher() {
        if(System.getProperty("command.test") != null) {
        try {
        System.out.println("makeMatcher");
        Model model = ModelFactory.createDefaultModel();
        Map<String, Object> params = new HashMap<>();
        params.put("command", "python3 src/test/resources/test-matcher.py");
        Command instance = new Command();
        Matcher matcher = instance.makeMatcher(params);
        AlignmentSet as = new AlignmentSet();
        as.add(new Alignment(new URIRes("http://www.example.com/id1", "left"),new URIRes("http://www.example.com/id1", "right"),0.5));
        as.add(new Alignment(new URIRes("http://www.example.com/id1", "left"),new URIRes("http://www.example.com/id2", "right"),0.5));
        as.add(new Alignment(new URIRes("http://www.example.com/id2", "left"),new URIRes("http://www.example.com/id2", "right"),0.5));
        AlignmentSet result = matcher.align(as);
        AlignmentSet expResult = new AlignmentSet();
        expResult.add(new Alignment(new URIRes("http://www.example.com/id1", "left"),new URIRes("http://www.example.com/id1", "right"),0.5));
        expResult.add(new Alignment(new URIRes("http://www.example.com/id2", "left"),new URIRes("http://www.example.com/id2", "right"),0.5));
        assertEquals(expResult, result);
        } catch(ExternalCommandException x) {
            x.printStackTrace();
        }
        }
    }

}