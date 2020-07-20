package org.insightcentre.uld.naisc.main;

import java.io.File;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.STDERR;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class TrainTest {
    
    public TrainTest() {
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
     * Test of execute method, of class Train.
     */
    @Test
    public void testExecute() throws Exception {
        System.out.println("execute");
        File leftFile = new File("src/test/resources/left.nt");
        File rightFile = new File("src/test/resources/right.nt");
        File alignment = new File("src/test/resources/align.nt");
        File configuration = new File("src/test/resources/simple.json");
        Train.execute("test", leftFile, rightFile, alignment, 5.0, configuration, STDERR, new DefaultDatasetLoader(), null);
    }

}
