package org.insightcentre.uld.naisc.main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * The standard dataset loader
 *
 * @author John McCrae
 */
public class DefaultDatasetLoader implements DatasetLoader {

    @Override
    public Dataset fromFile(File file, String name) throws IOException {

        final Model model = ModelFactory.createDefaultModel();
        model.read(new FileReader(file), file.toURI().toString(), "riot");
        return new Dataset() {
            @Override
            public Option<Model> asModel() {
                return new Some<>(model);
            }

            @Override
            public Option<URL> asEndpoint() {
                return new None<>();
            }
        };
    }

    @Override
    public Dataset fromEndpoint(final URL endpoint) {
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
        return new Dataset() {
            @Override
            public Option<Model> asModel() {
                return new Some<>(combined);
            }

            @Override
            public Option<URL> asEndpoint() {
                return new None<>();
            }
        };
    }

}
