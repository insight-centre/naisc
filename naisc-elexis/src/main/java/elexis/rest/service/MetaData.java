package elexis.rest.service;

import java.util.ArrayList;

/**
 * Stores the MetaData about the dictionary
 * @author Suruchi Gupta
 */
public class MetaData {
    private String release;
    private String sourceLanguage;
    private ArrayList<String> targetLanguage = new ArrayList<>();
    private ArrayList<String> genre = new ArrayList<>();
    private String license;
    private ArrayList<Creator> creator = new ArrayList<>();
    private ArrayList<Publisher> publisher = new ArrayList<>();

    /**
     * Set release type
     * @param release
     */
    public void setRelease(String release) {
        this.release = release;
    }

    /**
     * Get the release type
     * @return release
     */
    public String getRelease(){
        return this.release;
    }

    /**
     * Set the Source Language Code
     * @param sourceLanguageCode
     */
    public void setSourceLanguage(String sourceLanguageCode) {
        this.sourceLanguage = sourceLanguageCode;
    }

    /**
     * Get the Source Language code
     * @return sourceLanguageCode
     */
    public String getSourceLanguage() {
        return this.sourceLanguage;
    }

    /**
     * Set list of all target language code
     * @param targetLanguage
     */
    public void setTargetLanguage(ArrayList targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    /**
     * Get list of all target language code
     * @return targetLanguageCode
     */
    public ArrayList getTargetLanguage() {
        return targetLanguage;
    }

    /**
     * Set list of all genre
     * @param genre
     */
    public void setGenre(ArrayList genre) {
        this.genre = genre;
    }

    /**
     * Get the list of all genre
     * @return genre
     */
    public ArrayList getGenre() {
        return genre;
    }

    /**
     * Set license URL
     * @param license
     */
    public void setLicense(String license) {
        this.license = license;
    }

    /**
     * Get license URL
     * @return licenseURL
     */
    public String getLicense(){
        return license;
    }

    /**
     * Set the list of creator of the dictionary
     * @param creator
     */
    public void setCreator(ArrayList<Creator> creator) {
        this.creator = creator;
    }

    /**
     * Get the list of all creator
     * @return creator
     */
    public ArrayList getCreator(){
        return creator;
    }

    /**
     * Set the list of publisher of the dictionary
     * @param publisher
     */
    public void setPublisher(ArrayList<Publisher> publisher) {
        this.publisher = publisher;
    }

    /**
     * Get the list of all publisher
     * @return publisher
     */
    public ArrayList getPublisher(){
        return publisher;
    }

}