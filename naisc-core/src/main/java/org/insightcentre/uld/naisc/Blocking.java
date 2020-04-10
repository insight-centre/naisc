package org.insightcentre.uld.naisc;

public class Blocking {
    private final Resource entity1;
    private final Resource entity2;

    public Blocking(Resource entity1, Resource entity2) {
        this.entity1 = entity1;
        this.entity2 = entity2;
    }

    public Resource getEntity1() {
        return entity1;
    }

    public Resource getEntity2() {
        return entity2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Blocking blocking = (Blocking) o;

        if (entity1 != null ? !entity1.equals(blocking.entity1) : blocking.entity1 != null) return false;
        return entity2 != null ? entity2.equals(blocking.entity2) : blocking.entity2 == null;
    }

    @Override
    public int hashCode() {
        int result = entity1 != null ? entity1.hashCode() : 0;
        result = 31 * result + (entity2 != null ? entity2.hashCode() : 0);
        return result;
    }
}
