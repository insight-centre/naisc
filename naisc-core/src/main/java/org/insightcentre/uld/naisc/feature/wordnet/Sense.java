package org.insightcentre.uld.naisc.feature.wordnet;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A sense in a wordnet
 * @author John McCrae 
 */
public class Sense {
    public final String id;
    public final String synset;
    public final List<Relation> relations;

    public Sense(String id, String synset, List<Relation> relations) {
        this.id = id;
        this.synset = synset;
        this.relations = Collections.unmodifiableList(relations);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + Objects.hashCode(this.synset);
        hash = 37 * hash + Objects.hashCode(this.relations);
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
        final Sense other = (Sense) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.synset, other.synset)) {
            return false;
        }
        if (!Objects.equals(this.relations, other.relations)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Sense{" + "id=" + id + ", synset=" + synset + ", relations=" + relations + '}';
    }

    public static final class Relation {
        public final String relType;
        public final String target;

        public Relation(String relType, String target) {
            this.relType = relType;
            this.target = target;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.relType);
            hash = 89 * hash + Objects.hashCode(this.target);
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
            final Relation other = (Relation) obj;
            if (!Objects.equals(this.relType, other.relType)) {
                return false;
            }
            if (!Objects.equals(this.target, other.target)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Relation{" + "relType=" + relType + ", target=" + target + '}';
        }

        
    }
    
}
