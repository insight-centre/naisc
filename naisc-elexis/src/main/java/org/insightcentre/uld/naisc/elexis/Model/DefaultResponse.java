package org.insightcentre.uld.naisc.elexis.Model;

import java.util.ArrayList;

/**
 * Stores the response for default Linking API
 * @author Suruchi Gupta
 */
public class DefaultResponse {
    private ArrayList<String> dictionaries = new ArrayList<String>();

    /**
     * Get the list of available dictionaries
     * @return dictionaries
     */
    public ArrayList<String> getDictionaries() {
        return dictionaries;
    }

    /**
     * Set the list of available dictionaries
     * @param dictionaries
     */
    public void setDictionaries(ArrayList<String> dictionaries) {
        this.dictionaries = dictionaries;
    }
}
