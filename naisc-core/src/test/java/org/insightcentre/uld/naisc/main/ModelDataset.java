package org.insightcentre.uld.naisc.main;

import java.net.URL;
import org.apache.jena.rdf.model.Model;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;
import org.insightcentre.uld.naisc.util.None;

/**
 *
 * @author John McCrae
 */
public class ModelDataset implements Dataset{
    private final Model model;

    public ModelDataset(Model model) {
        this.model = model;
    }
    
    

    @Override
    public Option<Model> asModel() {
        return new Some<>(model);
    }

    @Override
    public Option<URL> asEndpoint() {
        return new None<>();
    }

}
