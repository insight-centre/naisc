package org.insightcentre.uld.naisc.meas;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.adapters.RDFReaderRIOT;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * The Meas loader also creates a SPARQL endpoint for the datasets
 *
 * @author John McCrae
 */
public class MeasDatasetLoader implements DatasetLoader<MeasDatasetLoader.MeasDataset>, AutoCloseable {

    private Set<String> models = new HashSet<>();
    private final String requestURL;

    public MeasDatasetLoader(String requestURL) {
        this.requestURL = requestURL.endsWith("/") ? requestURL : (requestURL + "/");
    }

    @Override
    public Dataset fromFile(File file, String name) throws IOException {
        final Model model = ModelFactory.createDefaultModel();
        if(file.getName().endsWith(".rdf")) {
            model.read(new FileReader(file), file.toURI().toString(), "RDF/XML");
        } else if(file.getName().endsWith(".ttl")) {
            model.read(new FileReader(file), file.toURI().toString(), "Turtle");
        } else if(file.getName().endsWith(".nt")) {
            model.read(new FileReader(file), file.toURI().toString(), "N-TRIPLES");
        } else {
            model.read(new FileReader(file), file.toURI().toString(), "RDF/XML");
        }

        SPARQLEndpointServlet.registerModel(name, model);
        models.add(name);
        return new MeasDataset(name, model);
    }

    @Override
    public Dataset fromEndpoint(URL endpoint) {
        return new DefaultDatasetLoader.EndpointDataset(endpoint, endpoint.toString());
    }

    @Override
    public MeasDataset combine(MeasDataset dataset1, MeasDataset dataset2, String name) {
        final Model combined = ModelFactory.createDefaultModel();
        final Model leftModel = dataset1.model;
        final Model rightModel = dataset2.model;
        combined.add(leftModel);
        combined.add(rightModel);
        SPARQLEndpointServlet.registerModel(name, combined);
        models.add(name);
        return new MeasDataset(name, combined);
    }

    @Override
    public void close() throws IOException {
        for (String model : models) {
            SPARQLEndpointServlet.deregisterModel(model);
        }
    }

    public class MeasDataset extends ModelDataset {

        private final String name;

        public MeasDataset(String name, Model model) {
            super(model, name);
            this.name = name;
        }

        @Override
        public Option<URL> asEndpoint() {
            try {
                return new Some<>(new URL(requestURL + "sparql/" + name));
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

}
