package org.insightcentre.uld.naisc.main;

import java.io.File;
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
public class MainTest {
    
    public MainTest() {
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
     * Test of execute method, of class Main.
     */
    @Test
    public void testExecute() throws Exception {
        System.out.println("execute");
        File leftFile = new File("src/test/resources/left.nt");
        File rightFile = new File("src/test/resources/right.nt");
        File configuration = new File("src/test/resources/simple.json");
        File outputFile = null;
        boolean outputXML = false;
        Main.execute(leftFile, rightFile, configuration, outputFile, outputXML, new Main.NoMonitor(), new DefaultDatasetLoader());
    }

    
}
