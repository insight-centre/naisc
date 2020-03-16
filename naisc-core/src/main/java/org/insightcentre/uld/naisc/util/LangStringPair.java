package org.insightcentre.uld.naisc.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.monnetproject.lang.Language;
import java.util.Objects;

/**
 * A string pair that also has a language for each string
 * @author John McCrae
 */
public class LangStringPair extends StringPair {

    public final Language lang1, lang2;

    @JsonCreator public LangStringPair(@JsonProperty("lang1") Language lang1, @JsonProperty("lang2") Language lang2, @JsonProperty("_1") String _1, @JsonProperty("_2") String _2) {
        super(_1, _2);
        this.lang1 = lang1;
        this.lang2 = lang2;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 53 * hash + Objects.hashCode(this.lang1);
        hash = 53 * hash + Objects.hashCode(this.lang2);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if(!super.equals(obj)) {
            return false;
        }
        final LangStringPair other = (LangStringPair) obj;
        if (!Objects.equals(this.lang1, other.lang1)) {
            return false;
        }
        if (!Objects.equals(this.lang2, other.lang2)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "LangStringPair{" + _1 + " lang1=" + lang1 + ", " + _2 + " lang2=" + lang2 + '}';
    }


    
}
