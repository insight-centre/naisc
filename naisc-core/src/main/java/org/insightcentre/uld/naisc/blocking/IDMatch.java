package org.insightcentre.uld.naisc.blocking;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Match identifiers according to their IDs. This is generally used when an
 * alignment is not required, but instead the alignment is already known and
 * the sentence similarity scores are required
 * 
 * @author John McCrae
 */
public class IDMatch implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> analysis, NaiscListener listener) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if(config.method == IDMatching.namespace && (config.leftNamespace == null || config.rightNamespace == null)) {
            throw new ConfigurationException("Method is namespace but namespaces are not set");
        }
        return new IDMatchImpl(config.method, config.leftNamespace, config.rightNamespace);
    }

    /**
     * The configuration of the ID match
     */
    public static class Configuration {
        /**
         * The method to match {@link IDMatching}
         */
        @ConfigurationParameter(description="The method to match", defaultValue = "\"endOfPath\"")
        public IDMatching method = IDMatching.endOfPath;
        /**
         * The namespace for matching elements in the left dataset
         */
        @ConfigurationParameter(description="The namespace for matching elements in the left dataset", defaultValue="\"\"")
        public String leftNamespace;
        /**
         * The namespace for matching elements in the right dataset
         */
        @ConfigurationParameter(description = "The namespace for matching elements in the right dataset", defaultValue="\"\"")
        public String rightNamespace;
        
    }
    
    /**
     * The methodology of matching IDs. The URL <code>http://www.example.com/foo/bar#baz</code>
     * matches as follows with these strategies
     * 
     * <table>
     * <caption>Matching results for IDMatch strategy</caption>
     * <tr>
     * <th>Method</th>
     * <th><code>http://www.example.com/foo/bar#baz</code></th>
     * <th><code>http://www.beispiel.de/path/to#baz</code></th>
     * <th><code>http://www.beispiel.de/path/bar#baz</code></th>
     * <th><code>http://www.beispiel.de/foo/bar#baz</code></th>
     * </tr>
     * <tr>
     * <td>Exact</td>
     * <td>Yes</td>
     * <td>No</td>
     * <td>No</td>
     * <td>No</td>
     * </tr>
     * <tr>
     * <td>fragment</td>
     * <td>Yes</td>
     * <td>Yes</td>
     * <td>Yes</td>
     * <td>Yes</td>
     * </tr>
     * <tr>
     * <td>End of Path</td>
     * <td>Yes</td>
     * <td>No</td>
     * <td>Yes</td>
     * <td>Yes</td>
     * </tr>
     * <tr>
     * <td style="max-width:300px;">Namespace (namespaces are <code>http://www.example.com/</code> and <code>http://www.beispiel.de</code>)</td>
     * <td>No</td>
     * <td>No</td>
     * <td>No</td>
     * <td>Yes</td>
     * </tr>
     * <tr>
     * </table>
     */
    public static enum IDMatching {
        /**
         * Require that the two URIs are exactly equal
         */
        exact,
        /**
         * Require the the fragment matches
         */
        fragment,
        /**
         * Requires that the last part of the path matches
         */
        endOfPath,
        /**
         * Requires that IDs match up to the last namespace
         */
        namespace
    }
    
    private static class RI {
        final Resource l;
        final ExtendedIterator<Resource> rs;

        public RI(Resource l, ExtendedIterator<Resource> rs) {
            this.l = l;
            this.rs = rs;
        }
        
    }
    
    private static class RIIter implements Iterator<Blocking> {
        private final ExtendedIterator<RI> riIter;
        private Resource l;
        private ExtendedIterator<Resource> rs;
        private final String left, right;

        public RIIter(ExtendedIterator<RI> riIter, String left, String right) {
            this.riIter = riIter;
            while(riIter.hasNext() && (rs == null || !rs.hasNext())) {
                RI ri = riIter.next();
                this.l = ri.l;
                this.rs = ri.rs;
            }
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean hasNext() {
            return this.rs.hasNext() || this.riIter.hasNext();
        }

        @Override
        public Blocking next() {
            while(riIter.hasNext() && !rs.hasNext()) {
                RI ri = riIter.next();
                this.l = ri.l;
                this.rs = ri.rs;
            }
            return new Blocking(l, rs.next(), left, right);
        }
    }
    
    private static class IDMatchImpl implements BlockingStrategy {
        private final IDMatching method;
        private final String leftNamespace;
        private final String rightNamespace;

        public IDMatchImpl(IDMatching method, String leftNamespace, String rightNamespace) {
            this.method = method;
            this.leftNamespace = leftNamespace;
            this.rightNamespace = rightNamespace;
        }
        
        private static String eop(String s) {
            if(s == null) {
                return null;
            } else if(s.contains("/")) {
                return s.substring(s.lastIndexOf("/") + 1);
            } else if (s.contains("\\")) {
                return s.substring(s.lastIndexOf("\\") + 1);
            } else {
                return s;
            }
        }
        
        private boolean matches(Resource r1, Resource r2) {
            switch(method) {
                case exact:
                    return r1.getURI().equals(r2.getURI());
                case fragment:
                    try {
                        String f1 = new URI(r1.getURI()).getFragment();
                        String f2 = new URI(r2.getURI()).getFragment();
                        return f1 == null || f2 == null || f1.equals(f2);
                    } catch(URISyntaxException x) {
                        return false;
                    }
                case endOfPath:
                    try {
                        String p1 = eop(new URI(r1.getURI()).getPath());
                        String p2 = eop(new URI(r2.getURI()).getPath());
                        return p1 != null && p2 != null && p1.equals(p2);
                    } catch(URISyntaxException x) {
                        return false;
                    }
                case namespace:
                    if(r1.getURI().startsWith(leftNamespace) && r2.getURI().endsWith(rightNamespace)) {
                        return r1.getURI().substring(leftNamespace.length()).equals(r2.getURI().substring(rightNamespace.length()));
                    } else {
                        return false;
                    }
                default:
                    throw new RuntimeException("Null strategy");
            }
        }

        @Override
        @SuppressWarnings("Convert2Lambda")
        public Collection<Blocking> block(final Dataset left, final Dataset right, NaiscListener log) {
            return new AbstractCollection<Blocking>() {
                @Override
                public Iterator<Blocking> iterator() {
                    ExtendedIterator<RI> ris = left.listSubjects().filterKeep(x -> x.isURIResource())
                            .mapWith(x -> {
                                return new RI(x, right.listSubjects().filterKeep(y -> y.isURIResource() && matches(x,y)));
                            });
                    return new RIIter(ris, left.id(), right.id());
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
    
}
