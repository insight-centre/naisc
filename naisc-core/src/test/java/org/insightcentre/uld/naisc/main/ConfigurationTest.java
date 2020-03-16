package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class ConfigurationTest {
    @Test
    public void testReadConfig() throws IOException {
        String config = "{\n" +
"    \"blocking\": {\n" +
"        \"name\": \"blocking.ApproximateStringMatching\",\n" +
"        \"maxMatches\": 5,\n" +
"        \"property\": \"http://www.w3.org/2000/01/rdf-schema#label\"\n" +
"    },\n" +
"    \"lenses\": [{\n" +
"        \"name\": \"lens.Label\"\n" +
"    }],\n" +
"    \"textFeatures\": [{\n" +
"        \"name\": \"feature.BasicString\"\n" +
"    }],\n" +
"    \"scorers\": [{\n" +
"        \"name\": \"scorer.Average\",\n" +
"        \"softmax\": true\n" +
"    }],\n" +
"    \"matcher\": {\n" +
"        \"name\": \"matcher.UniqueAssignment\"\n" +
"    },\n" +
"    \"description\": \"Useful only for testing, runs very quickly but produces very poor results\"\n" +
"}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.readValue(config, Configuration.class);
    }
    
    
    @Test
    public void testRoundTrip() throws IOException {
        String config = "{\n" +
"    \"blocking\": {\n" +
"        \"name\": \"blocking.ApproximateStringMatching\",\n" +
"        \"maxMatches\": 5,\n" +
"        \"property\": \"http://www.w3.org/2000/01/rdf-schema#label\"\n" +
"    },\n" +
"    \"lenses\": [{\n" +
"        \"name\": \"lens.Label\"\n" +
"    }],\n" +
"    \"textFeatures\": [{\n" +
"        \"name\": \"feature.BasicString\"\n" +
"    }],\n" +
"    \"scorers\": [{\n" +
"        \"name\": \"scorer.Average\",\n" +
"        \"softmax\": true\n" +
"    }],\n" +
"    \"matcher\": {\n" +
"        \"name\": \"matcher.UniqueAssignment\"\n" +
"    },\n" +
"    \"description\": \"Useful only for testing, runs very quickly but produces very poor results\"\n" +
"}";
        ObjectMapper mapper = new ObjectMapper();
        Configuration c1 = mapper.readValue(config, Configuration.class);
        String s = mapper.writeValueAsString(c1);
        Configuration c2 = mapper.readValue(s, Configuration.class);
        assertEquals(c1, c2);
    }
    
    //@Test
    public void testConfig() throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        Configuration c1 = mapper.readValue(new File("../configs/ontolex-default.json"), Configuration.class);
    }
}
