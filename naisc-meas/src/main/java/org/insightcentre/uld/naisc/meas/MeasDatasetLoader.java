package org.insightcentre.uld.naisc.meas;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * The Meas loader also creates a SPARQL endpoint for the datasets
 * @author John McCrae
 */
public class MeasDatasetLoader implements DatasetLoader, AutoCloseable {
    private Set<String> models = new HashSet<>();
    private final String requestURL;

    public MeasDatasetLoader(String requestURL) {
        this.requestURL = requestURL.endsWith("/") ? requestURL : (requestURL + "/");
    }
    
    @Override
    public Dataset fromFile(File file, String name) throws IOException {
        final Model model = ModelFactory.createDefaultModel();
        model.read(new FileReader(file), file.toURI().toString(), "riot");
        SPARQLEndpoint.registerModel(name, model);
        models.add(name);
        return new Dataset() {
            @Override
            public Option<Model> asModel() {
                return new Some<>(model);
            }

            @Override
            public Option<URL> asEndpoint() {
                try {
                    return new Some<>(new URL(requestURL + "sparql/" + name));
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    @Override
    public Dataset fromEndpoint(URL endpoint) {
        return new Dataset() {
            @Override
            public Option<Model> asModel() {
                return new None<>();
            }

            @Override
            public Option<URL> asEndpoint() {
                return new Some<>(endpoint);
            }
        };
    }

    @Override
    public Dataset combine(Dataset dataset1, Dataset dataset2, String name) {
        final Model combined = ModelFactory.createDefaultModel();
        final Model leftModel = dataset1.asModel().getOrExcept(new RuntimeException("Cannot combine SPARQL endpoints"));
        final Model rightModel = dataset2.asModel().getOrExcept(new RuntimeException("Cannot combine SPARQL endpoints"));
        combined.add(leftModel);
        combined.add(rightModel);
        SPARQLEndpoint.registerModel(name, combined);
        models.add(name);
        return new Dataset() {
            @Override
            public Option<Model> asModel() {
                return new Some<>(combined);
            }

            @Override
            public Option<URL> asEndpoint() {
                try {
                    return new Some<>(new URL(requestURL + "sparql/" + name));
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        for(String model : models) {
            SPARQLEndpoint.deregisterModel(model);
        }
    }
    
    

}
