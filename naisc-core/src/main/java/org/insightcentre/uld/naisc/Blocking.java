package org.insightcentre.uld.naisc;

public class Blocking {
    public final URIRes entity1;
    public final URIRes entity2;

    public Blocking(URIRes entity1, URIRes entity2) {
        this.entity1 = entity1;
        this.entity2 = entity2;
    }

    public Blocking(org.apache.jena.rdf.model.Resource r1, org.apache.jena.rdf.model.Resource r2, String dataset1, String dataset2) {
        this.entity1 = URIRes.fromJena(r1, dataset1);
        this.entity2 = URIRes.fromJena(r2, dataset2);
    }

    public URIRes getEntity1() {
        return entity1;
    }

    public URIRes getEntity2() {
        return entity2;
    }

    public org.apache.jena.rdf.model.Resource asJena1(Dataset dataset) {
        if(!dataset.id().equals(entity1.dataset)) {
            throw new IllegalArgumentException("URIRes not from same dataset");
        }
        return dataset.createResource(entity1.uri);
    }

    public org.apache.jena.rdf.model.Resource asJena2(Dataset dataset) {
        if(!dataset.id().equals(entity2.dataset)) {
            throw new IllegalArgumentException("URIRes not from same dataset");
        }
        return dataset.createResource(entity2.uri);
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

    @Override
    public String toString() {
        return "Blocking{" +
                "entity1=" + entity1 +
                ", entity2=" + entity2 +
                '}';
    }
}
