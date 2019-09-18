package org.insightcentre.uld.naisc.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A pair of strings
 * 
 * @author John McCrae
 */
public class StringPair extends Pair<String, String> {

    @JsonCreator public StringPair(@JsonProperty("_1") String _1, @JsonProperty("_2") String _2) {
        super(_1, _2);
    }
}
