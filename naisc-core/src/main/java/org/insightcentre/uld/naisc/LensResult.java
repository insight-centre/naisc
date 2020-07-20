package org.insightcentre.uld.naisc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.monnetproject.lang.Language;
import org.insightcentre.uld.naisc.util.LangStringPair;

public class LensResult {
    public final String string1, string2;
    public final Language lang1, lang2;
    public final String tag;

    @JsonCreator
    public LensResult(@JsonProperty("lang1") Language lang1, @JsonProperty("lang2") Language lang2, @JsonProperty("string1") String string1, @JsonProperty("string2") String string2, @JsonProperty("tag") String tag) {
        this.string1 = string1;
        this.string2 = string2;
        this.lang1 = lang1;
        this.lang2 = lang2;
        this.tag = tag;
    }

    public static LensResult fromLangStringPair(LangStringPair lsp, String tag) {
        return new LensResult(lsp.lang1, lsp.lang2, lsp._1, lsp._2, tag);
    }


    public String getString1() {
        return string1;
    }

    public String getString2() {
        return string2;
    }

    public Language getLang1() {
        return lang1;
    }

    public Language getLang2() {
        return lang2;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LensResult that = (LensResult) o;

        if (!string1.equals(that.string1)) return false;
        if (!string2.equals(that.string2)) return false;
        if (!lang1.equals(that.lang1)) return false;
        if (!lang2.equals(that.lang2)) return false;
        return tag != null ? tag.equals(that.tag) : that.tag == null;
    }

    @Override
    public int hashCode() {
        int result = string1.hashCode();
        result = 31 * result + string2.hashCode();
        result = 31 * result + lang1.hashCode();
        result = 31 * result + lang2.hashCode();
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LensResult{" +
                "string1='" + string1 + '\'' +
                ", string2='" + string2 + '\'' +
                ", lang1=" + lang1 +
                ", lang2=" + lang2 +
                ", tag='" + tag + '\'' +
                '}';
    }
}
