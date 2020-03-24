package org.insightcentre.uld.naisc.elexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration {

    @JsonProperty("some")
    org.insightcentre.uld.naisc.main.Configuration some;

    public org.insightcentre.uld.naisc.main.Configuration getSome() {
        return some;
    }

    public void setSome(org.insightcentre.uld.naisc.main.Configuration some) {
        this.some = some;
    }
}
