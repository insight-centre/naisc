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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.NaiscListener.Stage;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Some;

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
     * @param partialSoln A partial solution
     * @param outputXML If true output XML
     * @param monitor Listener for status updates
     * @param loader The loader of datasets
     */
    @SuppressWarnings("UseSpecificCatch")
    public static void execute(String name, File leftFile, File rightFile, File configuration,
            File outputFile, Option<File> partialSoln, boolean outputXML, ExecuteListener monitor,
            DatasetLoader loader) {
        try {
            monitor.updateStatus(Stage.INITIALIZING, "Reading Configuration");
            final Configuration config = mapper.readValue(configuration, Configuration.class);
            execute(name, leftFile, rightFile, config, outputFile, partialSoln, outputXML, monitor, loader);
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
     * @param partialSoln A partial solution
     * @param outputXML If true output XML
     * @param monitor Listener for status updates
     * @param loader The loader of datasets
     */
    @SuppressWarnings("UseSpecificCatch")
    public static void execute(String name, File leftFile, File rightFile, Configuration config,
            File outputFile, Option<File> partialSoln, boolean outputXML, ExecuteListener monitor, DatasetLoader loader) {
        try {
            AlignmentSet finalAlignment = execute(name, leftFile, rightFile, config, partialSoln, monitor, loader);
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
     *
     * @param name The identifier for this run
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param config The configuration
     * @param partialSoln A partial solution
     * @param monitor Listener for status updates
     * @param loader The loader of datasets
     * @return The alignment
     */
    @SuppressWarnings("UseSpecificCatch")
    public static AlignmentSet execute(String name, File leftFile, File rightFile, Configuration config,
            Option<File> partialSoln, ExecuteListener monitor, DatasetLoader loader) {
        try {

            final Option<AlignmentSet> partial;
            if (partialSoln.has()) {
                monitor.updateStatus(Stage.INITIALIZING, "Loading partial solution");
                partial = new Some<>(Train.readAlignments(partialSoln.get()));
            } else {
                partial = new None<>();
            }

            return execute2(name, leftFile, rightFile, config, partial, monitor, loader);
        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            return null;
        }
    }

    /**
     * Execute NAISC
     *
     * @param name The identifier for this run
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param config The configuration
     * @param partialSoln A partial solution
     * @param monitor Listener for status updates
     * @param loader The loader of datasets
     * @return The alignment
     */
    @SuppressWarnings("UseSpecificCatch")
    public static AlignmentSet execute2(String name, File leftFile, File rightFile, Configuration config,
            Option<AlignmentSet> partialSoln, ExecuteListener monitor, DatasetLoader loader) {
        try {
            monitor.updateStatus(Stage.INITIALIZING, "Reading left dataset");
            Dataset leftModel = loader.fromFile(leftFile, name + "/left");

            monitor.updateStatus(Stage.INITIALIZING, "Reading right dataset");
            Dataset rightModel = loader.fromFile(rightFile, name + "/right");

            return execute(name, leftModel, rightModel, config, partialSoln, monitor, null, null, loader);

        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            return null;
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    public static AlignmentSet execute(String name, Dataset leftModel, Dataset rightModel, Configuration config,
            Option<AlignmentSet> partialSoln, ExecuteListener monitor, Set<Resource> left, Set<Resource> right, DatasetLoader loader) {
        try {
            Lazy<Analysis> analysis = Lazy.fromClosure(() -> {
                DatasetAnalyzer analyzer = new DatasetAnalyzer();
                return analyzer.analyseModel(leftModel.asModel().getOrExcept(new RuntimeException("Automatic analysis cannot be performed on SPARQL endpoints")), 
                        rightModel.asModel().getOrExcept(new RuntimeException("Automatic analysis cannot be performed on SPARQL endpoints")));
            });
            monitor.updateStatus(Stage.INITIALIZING, "Loading blocking strategy");
            BlockingStrategy blocking = config.makeBlockingStrategy(analysis);

            monitor.updateStatus(Stage.INITIALIZING, "Loading lenses");
            Dataset combined = loader.combine(leftModel, rightModel, name + "/combined");
            List<Lens> lenses = config.makeLenses(combined, analysis, monitor);

            monitor.updateStatus(Stage.INITIALIZING, "Loading Feature Extractors");
            List<TextFeature> textFeatures = config.makeTextFeatures();
            List<GraphFeature> dataFeatures = config.makeDataFeatures(combined);

            monitor.updateStatus(Stage.INITIALIZING, "Loading Scorers");
            List<Scorer> scorers = config.makeScorer();

            monitor.updateStatus(Stage.INITIALIZING, "Loading Matcher");
            Matcher matcher = config.makeMatcher();

            monitor.updateStatus(Stage.BLOCKING, "Blocking");
            Iterable<Pair<Resource, Resource>> blocks = blocking.block(leftModel, rightModel, monitor);
            if (left != null && right != null) {
                blocks = new FilterBlocks(blocks, left, right);
            }

            monitor.updateStatus(Stage.SCORING, "Scoring");
            //int count = 0;
            final AtomicInteger count = new AtomicInteger(0);
            ConcurrentLinkedQueue<Alignment> alignments = new ConcurrentLinkedQueue<>();
            ExecutorService executor = new ThreadPoolExecutor(config.nThreads, config.nThreads, 0,
                    TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1000),
                    new ThreadPoolExecutor.CallerRunsPolicy());
            boolean blocksEmpty = true;
            for (Pair<Resource, Resource> block : blocks) {
                blocksEmpty = false;
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (block._1.getURI() == null || block._1.getURI().equals("")
                                    || block._2.getURI() == null || block._2.getURI().equals("")) {
                                System.err.println(block._1);
                                System.err.println(block._2);
                                throw new RuntimeException("Resource without URI");
                            }
                            monitor.addBlock(block._1, block._2);
                            int c = count.incrementAndGet();
                            if (c % 1000 == 0) {
                                monitor.updateStatus(Stage.SCORING, "Scoring (" + c + " done)");
                            }
                            FeatureSet featureSet = new FeatureSet(block._1, block._2);
                            for (Lens lens : lenses) {
                                Option<LangStringPair> oFacet = lens.extract(block._1, block._2, monitor);
                                if (!oFacet.has()) {
                                    monitor.updateStatus(Stage.SCORING, String.format("Lens produced no label for %s %s", block._1, block._2));
                                } else {
                                    monitor.addLensResult(block._1, block._2, lens.id(), oFacet.get());
                                }
                                LangStringPair facet = oFacet.getOrElse(EMPTY_LANG_STRING_PAIR);
                                for (TextFeature featureExtractor : textFeatures) {
                                    if (featureExtractor.tags() == null || lens.tag() == null
                                            || featureExtractor.tags().contains(lens.tag())) {
                                        double[] features = featureExtractor.extractFeatures(facet, monitor);
                                        featureSet = featureSet.add(new FeatureSet(featureExtractor.getFeatureNames(),
                                                lens.id(), features, block._1, block._2));
                                    }
                                }
                            }
                            for (GraphFeature feature : dataFeatures) {
                                double[] features = feature.extractFeatures(block._1, block._2, monitor);
                                featureSet = featureSet.add(new FeatureSet(feature.getFeatureNames(), feature.id(), features, block._1, block._2));
                            }
                            if(featureSet.isEmpty()) {
                                monitor.message(Stage.SCORING, NaiscListener.Level.CRITICAL, "An empty feature set was created");
                            }
                            for (Scorer scorer : scorers) {
                                double score = scorer.similarity(featureSet, monitor);
                                alignments.add(new Alignment(block._1, block._2, score, scorer.relation()));
                            }
                        } catch (Exception x) {
                            monitor.updateStatus(Stage.FAILED, String.format("Failed to score %s <-> %s due to %s (%s)\n", block._1, block._2, x.getMessage(), x.getClass().getName()));
                            x.printStackTrace();

                        }

                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.DAYS);
            monitor.updateStatus(Stage.SCORING, String.format("Scored %d pairs", count.get()));
            if(blocksEmpty) {
                monitor.message(Stage.BLOCKING, NaiscListener.Level.CRITICAL, "Blocking failed to extract any pairs");
            } else if(count.get() == 0) {
                monitor.message(Stage.SCORING, NaiscListener.Level.CRITICAL, "Failed to extract any pairs!");
            }

            for (Scorer scorer : scorers) {
                scorer.close();
            }

            AlignmentSet alignmentSet = new AlignmentSet();
            alignmentSet.addAll(alignments);

            monitor.updateStatus(Stage.MATCHING, "Matching");
            if (partialSoln.has()) {
                return matcher.alignWith(alignmentSet, partialSoln.get(), monitor);
            } else {
                return matcher.align(alignmentSet, monitor);
            }
        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            monitor.message(Stage.FAILED, NaiscListener.Level.CRITICAL, "The process failed due to an exception: " + x.getClass().getName() + ": " + x.getMessage());
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
                    accepts("p", "A partial matching that will be used as the basis for matching").withRequiredArg().ofType(File.class);
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
            final Option<File> partialSoln = os.valueOf("p") == null ? new None<>() : new Some<>((File) os.valueOf("p"));
            //final boolean example = os.has("x");
            final boolean outputXML = os.has("xml");
            //final boolean hard = !os.has("easy");
            execute("naisc", left, right, configuration, outputFile,
                    partialSoln, outputXML,
                    os.valueOf("q").equals(Boolean.TRUE)
                    ? ExecuteListeners.NONE : ExecuteListeners.STDERR,
                    new DefaultDatasetLoader());
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(-1);
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
                    if (p == null) {
                        throw new NoSuchElementException();
                    }
                    Pair<Resource, Resource> rval = p;
                    p = advance();
                    return rval;
                }

                private Pair<Resource, Resource> advance() {
                    while (i.hasNext()) {
                        Pair<Resource, Resource> x = i.next();
                        if (left.contains(x._1) && right.contains(x._2)) {
                            return x;
                        }
                    }
                    return null;
                }
            };
        }

    }
}
