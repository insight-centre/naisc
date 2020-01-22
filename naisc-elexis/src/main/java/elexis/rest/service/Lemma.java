package elexis.rest.service;

import java.util.ArrayList;
import java.util.function.IntFunction;

/**
 * Stores the detail of each Lemma
 * @author Suruchi Gupta
 */
public class Lemma{
    private String release;
    private String lemma;
    private String id;
    private ArrayList<String> partOfSpeech = new ArrayList<>();
    private ArrayList<String> formats = new ArrayList<>();

    /**
     * Set release type
     * @param release
     */
    public void setRelease(String release) {
        this.release = release;
    }

    /**
     * Get release type
     * @return release
     */
    public String getRelease() {
        return release;
    }

    /**
     * Set lemma name
     * @param lemma
     */
    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    /**
     * Get lemma name
     * @return lemma
     */
    public String getLemma() {
        return lemma;
    }

    /**
     * Set id
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get id
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the list of Part of Speech tag for the lemma
     * @param partOfSpeech
     */
    public void setPartOfSpeech(ArrayList<String> partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    /**
     * Get the list of Part of Speech tag for the lemma
     * @return PartOfSpeech
     */
    public ArrayList<String> getPartOfSpeech() {
        return partOfSpeech;
    }

    /**
     * Set the list of formats for the lemma
     * @param formats
     */
    public void setFormats(ArrayList<String> formats) {
        this.formats = formats;
    }

    /**
     * Get the list of formats for the lemma
     * @return format
     */
    public ArrayList<String> getFormats() {
        return formats;
    }
}