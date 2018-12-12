package org.insightcentre.uld.naisc.feature.wordnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A lexical entry in a wordnet.
 * @author John McCrae
 */
public class LexicalEntry {
    public final String writtenFrom;
    public final String partOfSpeech;
    public final List<Sense> senses;

    public LexicalEntry(String writtenFrom, String partOfSpeech, List<Sense> senses) {
        this.writtenFrom = writtenFrom;
        this.partOfSpeech = partOfSpeech;
        this.senses = Collections.unmodifiableList(senses);
    }

    public List<Synset> synsets(WordNetData wordnet) {
        List<Synset> synsets = new ArrayList<>();
        for(Sense s : senses) {
            synsets.add(wordnet.lookupSynset(s.synset));
        }
        return synsets;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.writtenFrom);
        hash = 79 * hash + Objects.hashCode(this.partOfSpeech);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LexicalEntry other = (LexicalEntry) obj;
        if (!Objects.equals(this.writtenFrom, other.writtenFrom)) {
            return false;
        }
        if (!Objects.equals(this.partOfSpeech, other.partOfSpeech)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "LexicalEntry{" + "writtenFrom=" + writtenFrom + ", partOfSpeech=" + partOfSpeech + ", senses=" + senses + '}';
    }

    

}
