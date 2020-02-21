package elexis.rest.service;

/**
 * Stores details of creator of the dictionary
 * @author Suruchi Gupta
 */
public class Creator {
    private String name;
    private String email;

    /**
     * Set name of creator
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get name of the Creator
     * @return name
     */
    public String getName(){
        return name;
    }

    /**
     * Set the email of the creator
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Get email of the Creator
     * @return email
     */
    public String getEmail(){
        return email;
    }
}