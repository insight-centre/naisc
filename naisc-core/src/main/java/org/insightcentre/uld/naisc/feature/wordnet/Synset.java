package org.insightcentre.uld.naisc.feature.wordnet;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A synset in a wordnet
 * @author John McCrae
 */
public class Synset {
    public final String id;
    public final String definition;
    public final List<Relation> relations;

    public Synset(String id, String definition, List<Relation> relations) {
        this.id = id;
        this.definition = definition;
        this.relations = Collections.unmodifiableList(relations);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + Objects.hashCode(this.id);
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
        final Synset other = (Synset) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Synset{" + "id=" + id + ", definition=" + definition + ", relations=" + relations + '}';
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
            int hash = 3;
            hash = 61 * hash + Objects.hashCode(this.relType);
            hash = 61 * hash + Objects.hashCode(this.target);
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
