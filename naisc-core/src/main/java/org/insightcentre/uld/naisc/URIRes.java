package org.insightcentre.uld.naisc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.jena.rdf.model.Resource;

import java.util.HashMap;
import java.util.Map;

public class URIRes {
    public final String uri;
    public final String dataset;

    public @JsonCreator URIRes(@JsonProperty("uri") String uri, @JsonProperty("dataset") String dataset) {
        if(uri == null || uri.equals("")) {
            throw new IllegalArgumentException("URI cannot be null or empty");
        }
        if(dataset == null || dataset.equals("")) {
            throw new IllegalArgumentException("Dataset cannot be null or empty");
        }
        this.uri = uri;
        this.dataset = dataset;
    }

    public org.apache.jena.rdf.model.Resource toJena(Dataset dataset) {
        return dataset.createResource(uri);
    }

    public static URIRes fromJena(org.apache.jena.rdf.model.Resource res, String dataset) {
        return new URIRes(res.getURI(), dataset);
    }

    @JsonIgnore
    public String getURI() {
        return uri;
    }

    public String getDataset() {
        return dataset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        URIRes resource = (URIRes) o;

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
        return "URIRes{" +
                "uri='" + uri + '\'' +
                ", dataset='" + dataset + '\'' +
                '}';
    }
}
