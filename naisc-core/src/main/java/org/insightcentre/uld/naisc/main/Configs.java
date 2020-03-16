package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Utility function for loading configurations
 * 
 * @author John McCrae
 */
public class Configs {

    private Configs() {
    }
    
    
    private static ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    public static <C> C loadConfig(Class<C> configClass, Map<String, Object> params) {
        return mapper.convertValue(params, configClass);
    }
}
