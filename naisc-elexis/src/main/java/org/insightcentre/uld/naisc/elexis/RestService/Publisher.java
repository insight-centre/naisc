package org.insightcentre.uld.naisc.elexis.RestService;

/**
 * Stores details of publisher of the dictionary
 * @author Suruchi Gupta
 */
public class Publisher {
    private String name;

    /**
     * Set name of publisher
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get name of the publisher
     * @return name
     */
    public String getName() {
        return name;
    }
}
