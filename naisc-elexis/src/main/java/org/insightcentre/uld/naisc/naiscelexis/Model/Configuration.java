package org.insightcentre.uld.naisc.naiscelexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration {

    @JsonProperty("some")
    String some;

    public String getSome() {
        return some;
    }

    public void setSome(String some) {
        this.some = some;
    }
}
