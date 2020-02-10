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
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.main.Train;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * A blocking strategy that simply replays from an existing file
 * 
 * @author John McCrae
 */
public class Predefined implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> analysis, NaiscListener listener) {
        final Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        final File links = config.links == null ? null : new File(config.links);
        if(links == null || !links.exists()) 
            throw new ConfigurationException("The links file is not specified or does not exist");
        return new PredefinedImpl(links);
    }
    /**
     * The configuration of the predefined mapping
     */
    public static class Configuration {
        /** The path to the file containing the links to playback */
        @ConfigurationParameter(description = "The path to the file containing the links to produce")
        public String links;
    }
    
    private static class PredefinedImpl implements BlockingStrategy {
        private final File links;

        public PredefinedImpl(File links) {
            this.links = links;
        }

        @Override
        public Iterable<Pair<Resource, Resource>> block(final Dataset _left, final Dataset _right, NaiscListener log) {
            try {
                final AlignmentSet as = Train.readAlignments(links);
                return new Iterable<Pair<Resource, Resource>>() {
                    @Override
                    public Iterator<Pair<Resource, Resource>> iterator() {
                        return new AlignmentSetIterator(as.iterator());
                    }
                };
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }
    }
    
    private static class AlignmentSetIterator implements Iterator<Pair<Resource, Resource>> {
        private final Iterator<Alignment> iter;

        public AlignmentSetIterator(Iterator<Alignment> iter) {
            this.iter = iter;
        }
        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Pair<Resource, Resource> next() {
            Alignment a = iter.next();
            return new Pair<>(a.entity1, a.entity2);
        }
        
        
    }

}
