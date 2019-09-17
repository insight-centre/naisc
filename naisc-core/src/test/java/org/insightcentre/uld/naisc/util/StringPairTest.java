package org.insightcentre.uld.naisc.util;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class StringPairTest {

    public StringPairTest() {
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

    @Test
    public void testSerializeDeserialize() throws Exception {
        StringPair sp = new StringPair("foo----bar", "baz");
        ObjectMapper om = new ObjectMapper();
        String serialized = om.writeValueAsString(sp);
        StringPair deserialzied = om.readValue(serialized, StringPair.class);
        assertEquals(sp, deserialzied);
    }

}