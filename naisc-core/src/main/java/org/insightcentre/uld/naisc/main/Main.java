package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.TextFeature;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.main.ExecuteListener.Stage;

/**
 * Create a mapping between two schemas
 *
 * @author John McCrae
 */
public class Main {

    public Main() {
    }

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
    final static ObjectMapper mapper = new ObjectMapper();

    private static final LangStringPair EMPTY_LANG_STRING_PAIR = new LangStringPair(Language.UNDEFINED, Language.UNDEFINED, "", "");

    /**
     * Execute NAISC
     *
     * @param name The identifier for this run
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param configuration The configuration file
     * @param outputFile The output file to write to (null for STDOUT)
     * @param outputXML If true output XML
     * @param monitor Listener for status updates
     * @param loader The loader of datasets
     */
    @SuppressWarnings("UseSpecificCatch")
    public static void execute(String name, File leftFile, File rightFile, File configuration,
            File outputFile, boolean outputXML, ExecuteListener monitor,
            DatasetLoader loader) {
        try {
            monitor.updateStatus(Stage.INITIALIZING, "Reading Configuration");
            final Configuration config = mapper.readValue(configuration, Configuration.class);
            execute(name, leftFile, rightFile, config, outputFile, outputXML, monitor, loader);
        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
        }
    }

    /**
     * Execute NAISC
     *
     * @param name The identifier for this run
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param config The configuration
     * @param outputFile The output file to write to (null for STDOUT)
     * @param outputXML If true output XML
     * @param monitor Listener for status updates
     * @param loader The loader of datasets
     */
    @SuppressWarnings("UseSpecificCatch")
    public static void execute(String name, File leftFile, File rightFile, Configuration config,
            File outputFile, boolean outputXML, ExecuteListener monitor, DatasetLoader loader) {
        try {
            AlignmentSet finalAlignment = execute(name, leftFile, rightFile, config, monitor, loader);
            monitor.updateStatus(Stage.FINALIZING, "Saving");
            if (outputXML) {
                finalAlignment.toXML(outputFile == null ? System.out : new PrintStream(outputFile));
            } else {
                finalAlignment.toRDF(outputFile == null ? System.out : new PrintStream(outputFile));
            }
            monitor.updateStatus(Stage.COMPLETED, "Done");

        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
        }
    }

    /**
     * Execute NAISC
     * @param name The identifier for this run
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param config The configuration
     * @param monitor Listener for status updates
     * @param loader The loader of datasets
     * @return The alignment
     */
    @SuppressWarnings("UseSpecificCatch")
    public static AlignmentSet execute(String name, File leftFile, File rightFile, Configuration config,
            ExecuteListener monitor, DatasetLoader loader) { 
        try {
            monitor.updateStatus(Stage.INITIALIZING, "Reading left dataset");
            Dataset leftModel = loader.fromFile(leftFile, name + "/left");

            monitor.updateStatus(Stage.INITIALIZING, "Reading right dataset");
            Dataset rightModel = loader.fromFile(rightFile, name + "/right");
            
            return execute(name, leftModel, rightModel, config, monitor, null, null, loader);
            
        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            return null;
        }
    }
    
    @SuppressWarnings("UseSpecificCatch")
    public static AlignmentSet execute(String name, Dataset leftModel, Dataset rightModel, Configuration config,
            ExecuteListener monitor, Set<Resource> left, Set<Resource> right, DatasetLoader loader) {
        try {
            
            monitor.updateStatus(Stage.INITIALIZING, "Loading blocking strategy");
            BlockingStrategy blocking = config.makeBlockingStrategy();

            monitor.updateStatus(Stage.INITIALIZING, "Loading lenses");
            Dataset combined = loader.combine(leftModel, rightModel, name +"/combined");
            List<Lens> lenses = config.makeLenses(combined);

            monitor.updateStatus(Stage.INITIALIZING, "Loading Feature Extractors");
            List<TextFeature> textFeatures = config.makeTextFeatures();
            List<GraphFeature> dataFeatures = config.makeDataFeatures(combined);

            monitor.updateStatus(Stage.INITIALIZING, "Loading Scorers");
            List<Scorer> scorers = config.makeScorer();

            monitor.updateStatus(Stage.INITIALIZING, "Loading Matcher");
            Matcher matcher = config.makeMatcher();

            monitor.updateStatus(Stage.BLOCKING, "Blocking");
            Iterable<Pair<Resource, Resource>> blocks = blocking.block(leftModel, rightModel);
            if(left != null && right != null) {
                blocks = new FilterBlocks(blocks, left, right);
            }

            monitor.updateStatus(Stage.SCORING, "Scoring");
            int count = 0;
            AlignmentSet alignments = new AlignmentSet();
            for (Pair<Resource, Resource> block : blocks) {
                if(block._1.getURI() == null || block._1.getURI().equals("") ||
                        block._2.getURI() == null || block._2.getURI().equals("")) {
                    System.err.println(block._1);
                    System.err.println(block._2);
                    throw new RuntimeException("Resource without URI");
                }
                monitor.addBlock(block._1, block._2);
                if (++count % 1000 == 0) {
                    monitor.updateStatus(Stage.SCORING, "Scoring (" + count + " done)");
                }
                FeatureSet featureSet = new FeatureSet(block._1, block._2);
                for (Lens lens : lenses) {
                    Option<LangStringPair> oFacet = lens.extract(block._1, block._2);
                    if (!oFacet.has()) {
                        monitor.updateStatus(Stage.SCORING, String.format("Lens produced no label for %s %s", block._1, block._2));
                    } else {
                        monitor.addLensResult(block._1, block._2, lens.id(), oFacet.get());
                    }
                    LangStringPair facet = oFacet.getOrElse(EMPTY_LANG_STRING_PAIR);
                    for (TextFeature featureExtractor : textFeatures) {
                        if (featureExtractor.tags() == null || lens.tag() == null
                                || featureExtractor.tags().contains(lens.tag())) {
                            double[] features = featureExtractor.extractFeatures(facet);
                            featureSet = featureSet.add(new FeatureSet(featureExtractor.getFeatureNames(),
                                    lens.id(), features, block._1, block._2));
                        }
                    }
                }
                for (GraphFeature feature : dataFeatures) {
                    double[] features = feature.extractFeatures(block._1, block._2);
                    featureSet = featureSet.add(new FeatureSet(feature.getFeatureNames(), feature.id(), features, block._1, block._2));
                }
                for (Scorer scorer : scorers) {
                    double score = scorer.similarity(featureSet);
                    alignments.add(new Alignment(block._1, block._2, score, scorer.relation()));
                }
            }

            for (Scorer scorer : scorers) {
                scorer.close();
            }

            monitor.updateStatus(Stage.MATCHING, "Matching");
            return matcher.align(alignments,monitor);
        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            return null;
        }
    }
    
    @SuppressWarnings("UseSpecificCatch")
    public static void main(String[] args) {
        try {
            final OptionParser p = new OptionParser() {
                {
                    accepts("o", "The file to write the output dataset to (STDOUT if omitted)").withRequiredArg().ofType(File.class);
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("xml", "Output as XML");
                    accepts("q", "Suppress output");
                    nonOptions("Two RDF files");
                }
            };
            final OptionSet os;
            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            // Validate options
            if (os.nonOptionArguments().size() != 2) {
                badOptions(p, "Wrong number of RDF files specified");
                return;
            }
            final File left = new File(os.nonOptionArguments().get(0).toString());
            if (!left.exists()) {
                badOptions(p, left.getName() + " does not exist");
                return;
            }
            final File right = new File(os.nonOptionArguments().get(1).toString());
            if (!right.exists()) {
                badOptions(p, right.getName() + " does not exist");
                return;
            }
            final File outputFile = (File) os.valueOf("o");
            final File configuration = (File) os.valueOf("c");
            if (configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist or not specified");
            }
            //final boolean example = os.has("x");
            final boolean outputXML = os.has("xml");
            //final boolean hard = !os.has("easy");
            execute("naisc", left, right, configuration, outputFile, outputXML,
                    os.valueOf("q").equals(Boolean.TRUE)
                    ? new NoMonitor() : new StdErrMonitor(),
                    new DefaultDatasetLoader());
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

    public static class StdErrMonitor implements ExecuteListener {

        @Override
        public void updateStatus(Stage stage, String message) {
            System.err.println("[" + stage + "]" + message);
        }

        @Override
        public void addLensResult(Resource id1, Resource id2, String lensId, LangStringPair res) {
        }

    }

    public static class NoMonitor implements ExecuteListener {

        @Override
        public void updateStatus(Stage stage, String message) {
        }

        @Override
        public void addLensResult(Resource id1, Resource id2, String lensId, LangStringPair res) {
        }

    }
    
    private static class FilterBlocks implements Iterable<Pair<Resource, Resource>> {
        private final Iterable<Pair<Resource, Resource>> iter;
        private final Set<Resource> left, right;

        public FilterBlocks(Iterable<Pair<Resource, Resource>> iter, Set<Resource> left, Set<Resource> right) {
            this.iter = iter;
            this.left = left;
            this.right = right;
        }

        @Override
        public Iterator<Pair<Resource, Resource>> iterator() {
            final Iterator<Pair<Resource, Resource>> i = iter.iterator();
            return new Iterator<Pair<Resource, Resource>>() {
                Pair<Resource, Resource> p = advance();
                @Override
                public boolean hasNext() {
                    return p != null;
                }

                @Override
                public Pair<Resource, Resource> next() {
                    if(p == null)
                        throw new NoSuchElementException();
                    Pair<Resource, Resource> rval = p;
                    p = advance();
                    return rval;
                }
                
                private Pair<Resource, Resource> advance() {
                    while(i.hasNext()) {
                        Pair<Resource, Resource> x = i.next();
                        if(left.contains(x._1) && right.contains(x._2)) {
                            return x;
                        }
                    }
                    return null;
                }
            };
        }
        
        
    }
}
