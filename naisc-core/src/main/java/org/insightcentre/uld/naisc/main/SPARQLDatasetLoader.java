package org.insightcentre.uld.naisc.main;

import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SPARQLDatasetLoader implements DatasetLoader<SPARQLDataset>  {
    private final int limit;

    public SPARQLDatasetLoader(int limit) {
        this.limit = limit;
    }

    @Override
    public Dataset fromFile(File file, String name) throws IOException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public Dataset fromEndpoint(URL endpoint) {
        return new SPARQLDataset(endpoint.toString(), endpoint.toString(), limit, null, null);
    }

    @Override
    public Dataset combine(SPARQLDataset dataset1, SPARQLDataset dataset2, String name) {
        return new CombinedDataset(dataset1, dataset2);
    }
}
