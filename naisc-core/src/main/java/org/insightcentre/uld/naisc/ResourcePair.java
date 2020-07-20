package org.insightcentre.uld.naisc;

import java.util.Objects;

/**
 * A pair of resources
 */
public class ResourcePair {
    public URIRes entity1;
    public URIRes entity2;

    public ResourcePair() {
    }

    public ResourcePair(URIRes entity1, URIRes entity2) {
        this.entity1 = entity1;
        this.entity2 = entity2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourcePair that = (ResourcePair) o;
        return Objects.equals(entity1, that.entity1) &&
                Objects.equals(entity2, that.entity2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity1, entity2);
    }
}
