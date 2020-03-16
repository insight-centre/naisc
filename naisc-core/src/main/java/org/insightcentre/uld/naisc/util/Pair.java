package org.insightcentre.uld.naisc.util;

import java.util.Objects;

/**
 * A pair of objects
 * 
 * @author John McCrae
 * @param <Entity1> The first entity type
 * @param <Entity2> The second entity type
 */
public class Pair<Entity1,Entity2> {
    public final Entity1 _1;
    public final Entity2 _2;

    public Pair(Entity1 _1, Entity2 _2) {
        this._1 = _1;
        this._2 = _2;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(this._1);
        hash = 13 * hash + Objects.hashCode(this._2);
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
        final Pair<?,?> other = (Pair<?,?>) obj;
        if (!Objects.equals(this._1, other._1)) {
            return false;
        }
        if (!Objects.equals(this._2, other._2)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EntityPair{" + "_1=" + _1 + ", _2=" + _2 + '}';
    }
    
    
}
