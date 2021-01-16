package org.insightcentre.uld.naisc.elexis.RestService;

import java.net.URL;

/**
 * Create an ELEXISRest object. This is implemented so it can be overriden in the tests
 */
public interface ELEXISRestFactory {
    public ELEXISRest make(URL endpoint);
}
