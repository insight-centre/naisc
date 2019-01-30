package org.insightcentre.uld.naisc.blocking;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.BlockingStrategyFactory;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.main.Train;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * A blocking strategy that simply replays from an existing file
 * 
 * @author John McCrae
 */
public class Predefined implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params) {
        final Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if(config.links == null || !config.links.exists()) 
            throw new ConfigurationException("The links file is not specified or does not exist");
        return new PredefinedImpl(config.links);
    }
    /**
     * The configuration of the predefined mapping
     */
    public static class Configuration {
        public File links;
    }
    
    private static class PredefinedImpl implements BlockingStrategy {
        private final File links;

        public PredefinedImpl(File links) {
            this.links = links;
        }

        @Override
        public Iterable<Pair<Resource, Resource>> block(final Model left, final Model right) {
            try {
                final AlignmentSet as = Train.readAlignments(links);
                return new Iterable<Pair<Resource, Resource>>() {
                    @Override
                    public Iterator<Pair<Resource, Resource>> iterator() {
                        return new AlignmentSetIterator(as.iterator(), left, right);
                    }
                };
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }
    }
    
    private static class AlignmentSetIterator implements Iterator<Pair<Resource, Resource>> {
        private final Iterator<Alignment> iter;
        private final Model left, right;

        public AlignmentSetIterator(Iterator<Alignment> iter, Model left, Model right) {
            this.iter = iter;
            this.left = left;
            this.right = right;
        }
        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Pair<Resource, Resource> next() {
            Alignment a = iter.next();
            return new Pair<>(left.createResource(a.entity1), right.createResource(a.entity2));
        }
        
        
    }

}
