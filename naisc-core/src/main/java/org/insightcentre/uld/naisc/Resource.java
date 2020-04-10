package org.insightcentre.uld.naisc;

import java.util.HashMap;
import java.util.Map;

public class Resource {
    public final String uri;
    public final String dataset;

    public Resource(String uri, String dataset) {
        this.uri = uri;
        this.dataset = dataset;
    }

    public static final Map<String, Dataset> datasets = new HashMap<>();

    public org.apache.jena.rdf.model.Resource toJena() {
        return datasets.get(dataset).createResource(uri);
    }

    public static Resource fromJena(org.apache.jena.rdf.model.Resource res, String dataset) {
        return new Resource(res.getURI(), dataset);
    }

    public String getUri() {
        return uri;
    }

    public String getDataset() {
        return dataset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource resource = (Resource) o;

        if (!uri.equals(resource.uri)) return false;
        return dataset.equals(resource.dataset);
    }

    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + dataset.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "uri='" + uri + '\'' +
                ", dataset='" + dataset + '\'' +
                '}';
    }
}
