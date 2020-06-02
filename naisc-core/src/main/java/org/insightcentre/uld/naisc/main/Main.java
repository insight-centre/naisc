package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.scorer.ModelNotTrainedException;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.NaiscListener.Stage;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import org.insightcentre.uld.naisc.matcher.Prematcher;
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
     * @param name          The identifier for this run
     * @param leftFile      The left RDF dataset to align
     * @param rightFile     The right RDF dataset to align
     * @param configuration The configuration file
     * @param outputFile    The output file to write to (null for STDOUT)
     * @param partialSoln   A partial solution
     * @param outputXML     If true output XML
     * @param monitor       Listener for status updates
     * @param loader        The loader of datasets
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
     * @param name        The identifier for this run
     * @param leftFile    The left RDF dataset to align
     * @param rightFile   The right RDF dataset to align
     * @param config      The configuration
     * @param outputFile  The output file to write to (null for STDOUT)
     * @param partialSoln A partial solution
     * @param outputXML   If true output XML
     * @param monitor     Listener for status updates
     * @param loader      The loader of datasets
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
     * @param name        The identifier for this run
     * @param leftFile    The left RDF dataset to align
     * @param rightFile   The right RDF dataset to align
     * @param config      The configuration
     * @param partialSoln A partial solution
     * @param monitor     Listener for status updates
     * @param loader      The loader of datasets
     * @return The alignment
     */
    @SuppressWarnings("UseSpecificCatch")
    public static AlignmentSet execute(String name, File leftFile, File rightFile, Configuration config,
                                       Option<File> partialSoln, ExecuteListener monitor, DatasetLoader loader) {
        try {

            final Option<AlignmentSet> partial;
            if (partialSoln.has()) {
                monitor.updateStatus(Stage.INITIALIZING, "Loading partial solution");
                partial = new Some<>(Train.readAlignments(partialSoln.get(), leftFile.getName(), rightFile.getName()));
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
     * @param name        The identifier for this run
     * @param leftFile    The left RDF dataset to align
     * @param rightFile   The right RDF dataset to align
     * @param config      The configuration
     * @param partialSoln A partial solution
     * @param monitor     Listener for status updates
     * @param loader      The loader of datasets
     * @return The alignment
     */
    @SuppressWarnings("UseSpecificCatch")
    public static AlignmentSet execute2(String name, File leftFile, File rightFile, Configuration config,
                                        Option<AlignmentSet> partialSoln, ExecuteListener monitor, DatasetLoader loader) throws ModelNotTrainedException {
        try {
            monitor.updateStatus(Stage.INITIALIZING, "Reading left dataset");
            Dataset leftModel = loader.fromFile(leftFile, "left");

            monitor.updateStatus(Stage.INITIALIZING, "Reading right dataset");
            Dataset rightModel = loader.fromFile(rightFile, "right");

            return execute(name, leftModel, rightModel, config, partialSoln, monitor, null, null, loader);

        } catch (ModelNotTrainedException x) {
            throw x;
        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            return null;
        }
    }

    /**
     * Execute NAISC, limiting the results to a gold set
     *
     * @param name        The identifier for this run
     * @param leftFile    The left RDF dataset to align
     * @param rightFile   The right RDF dataset to align
     * @param goldFile    The gold standard
     * @param config      The configuration
     * @param partialSoln A partial solution
     * @param monitor     Listener for status updates
     * @param loader      The loader of datasets
     * @return The alignment
     */
    @SuppressWarnings("UseSpecificCatch")
    public static AlignmentSet executeLimitedToGold(String name, File leftFile, File rightFile, File goldFile, Configuration config,
                                                    Option<AlignmentSet> partialSoln, ExecuteListener monitor, DatasetLoader loader) throws ModelNotTrainedException {
        try {
            monitor.updateStatus(Stage.INITIALIZING, "Reading left dataset");
            Dataset leftModel = loader.fromFile(leftFile, "left");

            monitor.updateStatus(Stage.INITIALIZING, "Reading right dataset");
            Dataset rightModel = loader.fromFile(rightFile, "right");

            monitor.updateStatus(Stage.INITIALIZING, "Reading gold dataset");
            AlignmentSet gold = Train.readAlignments(goldFile, leftModel.id(), rightModel.id());

            Set<URIRes> left = gold.stream().map(a -> a.entity1).collect(Collectors.toSet());
            Set<URIRes> right = gold.stream().map(a -> a.entity2).collect(Collectors.toSet());

            return execute(name, leftModel, rightModel, config, partialSoln, monitor, left, right, loader);
        } catch (ModelNotTrainedException x) {
            throw x;
        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            return null;
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    public static AlignmentSet execute(String name, Dataset leftModel, Dataset rightModel, Configuration config,
                                       Option<AlignmentSet> partialSoln, ExecuteListener monitor, Set<URIRes> left,
                                       Set<URIRes> right, DatasetLoader loader) throws ModelNotTrainedException {
        try {
            Lazy<Analysis> analysis = Lazy.fromClosure(() -> {
                DatasetAnalyzer analyzer = new DatasetAnalyzer();
                return analyzer.analyseModel(leftModel,
                        rightModel);
            });
            monitor.updateStatus(Stage.INITIALIZING, "Loading blocking strategy");
            BlockingStrategy blocking = config.makeBlockingStrategy(analysis, monitor);

            monitor.updateStatus(Stage.INITIALIZING, "Loading lenses");
            Dataset combined;
            if (loader != null) {
                combined = loader.combine(leftModel, rightModel, name + "/combined");
            } else {
                monitor.updateStatus(Stage.INITIALIZING, "Combining both models simply, querying is not enabled");
                combined = new CombinedDataset(leftModel, rightModel);
            }
            List<Lens> lenses = config.makeLenses(combined, analysis, monitor);

            monitor.updateStatus(Stage.INITIALIZING, "Loading Feature Extractors");
            List<TextFeature> textFeatures = config.makeTextFeatures();

            monitor.updateStatus(Stage.INITIALIZING, "Loading Scorers");
            Scorer scorer = config.makeScorer();

            monitor.updateStatus(Stage.INITIALIZING, "Loading Matcher");
            Matcher matcher = config.makeMatcher();

            Rescaler rescaler = config.makeRescaler();

            monitor.updateStatus(Stage.BLOCKING, "Blocking");
            Collection<Blocking> _blocks = blocking.block(leftModel, rightModel, monitor);
            final Collection<Blocking> blocks;
            if (config.ignorePreexisting) {
                _blocks = ExistingLinks.filterBlocking(_blocks, ExistingLinks.findPreexisting(leftModel, rightModel));
            }
            if (left != null && right != null) {
                blocks = new FilterBlocks(_blocks, left, right);
            } else {
                blocks = _blocks;
            }
            monitor.updateStatus(Stage.INITIALIZING, "Loading Graph Extractors");
            AlignmentSet prematch = new Prematcher().prematch(blocks, leftModel, rightModel);
            List<GraphFeature> dataFeatures = config.makeGraphFeatures(combined, analysis, prematch, monitor);

            monitor.updateStatus(Stage.SCORING, "Scoring");
            //int count = 0;
            final AtomicInteger count = new AtomicInteger(0);
            ConcurrentLinkedQueue<TmpAlignment> alignments = new ConcurrentLinkedQueue<>();
            ExecutorService executor = new ThreadPoolExecutor(config.nThreads, config.nThreads, 0,
                    TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1000),
                    new ThreadPoolExecutor.CallerRunsPolicy());
            final AtomicReference<ModelNotTrainedException> modelNotTrainedException = new AtomicReference<>();
            boolean blocksEmpty = true;
            for (Blocking block : blocks) {
                if(modelNotTrainedException.get() != null)
                    throw modelNotTrainedException.get();
                blocksEmpty = false;
                String property = config.noPrematching ? null : prematch.findLink(block.entity1, block.entity2);
                if(property != null) {
                    alignments.add(new TmpAlignment(block.entity1, block.entity2, new ScoreResult(1.0, property), property, null));
                } else {
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            Resource block1 = block.asJena1(leftModel), block2 = block.asJena2(rightModel);
                            try {
                                if (block1.getURI() == null || block1.getURI().equals("")
                                        || block2.getURI() == null || block2.getURI().equals("")) {
                                    System.err.println(block1);
                                    System.err.println(block2);
                                    throw new RuntimeException("URIRes without URI");
                                }
                                monitor.addBlock(block.entity1, block.entity2);
                                int c = count.incrementAndGet();
                                if (c % 1000 == 0) {
                                    monitor.updateStatus(Stage.SCORING, "Scoring (" + c + " done)");
                                }
                                FeatureSet featureSet = new FeatureSet();
                                boolean labelsProduced = false;
                                for (Lens lens : lenses) {
                                    for(LensResult facet : lens.extract(block.entity1, block.entity2, monitor)) {
                                        labelsProduced = true;
                                        monitor.addLensResult(block.entity1, block.entity2, facet.tag, facet);
                                        for (TextFeature featureExtractor : textFeatures) {
                                            if (featureExtractor.tags() == null || facet.tag == null
                                                    || featureExtractor.tags().contains(facet.tag)) {
                                                Feature[] features = featureExtractor.extractFeatures(facet, monitor);
                                                featureSet = featureSet.add(new FeatureSet(features,
                                                        facet.tag));
                                            }
                                        }
                                    }
                                }

                                if (!labelsProduced) {
                                    monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, String.format("Lens produced no label for %s %s", block1, block2));
                                }
                                for (GraphFeature feature : dataFeatures) {
                                    Feature[] features = feature.extractFeatures(block.entity1, block.entity2, monitor);
                                    featureSet = featureSet.add(new FeatureSet(features, feature.id()));
                                }
                                if (featureSet.isEmpty()) {
                                    monitor.message(Stage.SCORING, NaiscListener.Level.CRITICAL, "An empty feature set was created");
                                }
                                List<ScoreResult> scores = scorer.similarity(featureSet, monitor);
                                for(ScoreResult score : scores) {
                                    alignments.add(new TmpAlignment(block.entity1, block.entity2, score, score.getProperty(), config.includeFeatures ? featureSet : null));
                                }
                            } catch (ModelNotTrainedException x) {
                                modelNotTrainedException.set(x);
                            } catch (Exception x) {
                                monitor.updateStatus(Stage.FAILED, String.format("Failed to get probability %s <-> %s due to %s (%s)\n", block1, block2, x.getMessage(), x.getClass().getName()));
                                x.printStackTrace();

                            }

                        }
                    });
                }
            }
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.DAYS);
            if(modelNotTrainedException.get() != null)
                throw modelNotTrainedException.get();
            monitor.updateStatus(Stage.SCORING, String.format("Scored %d pairs", count.get()));
            if (blocksEmpty) {
                monitor.message(Stage.BLOCKING, NaiscListener.Level.CRITICAL, "Blocking failed to extract any pairs");
            } else if (count.get() == 0) {
                monitor.message(Stage.SCORING, NaiscListener.Level.CRITICAL, "Failed to extract any pairs!");
            }

            scorer.close();

            AlignmentSet alignmentSet = convertAligns(alignments, rescaler);

            monitor.updateStatus(Stage.MATCHING, "Matching");
            if (partialSoln.has()) {
                return matcher.alignWith(alignmentSet, partialSoln.get(), monitor);
            } else {
                return matcher.align(alignmentSet, monitor);
            }
        } catch (ModelNotTrainedException x) {
            throw x;
        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            monitor.message(Stage.FAILED, NaiscListener.Level.CRITICAL, "The process failed due to an exception: " + x.getClass().getName() + ": " + x.getMessage());
            return null;
        }
    }

    public static void executeMWSA(File mwsaFile, File configFile, File outputFile, ExecuteListener monitor, DatasetLoader loader) {

        try {
            monitor.updateStatus(Stage.INITIALIZING, "Reading Configuration");
            final Configuration config = mapper.readValue(configFile, Configuration.class);

            Model leftModel = ModelFactory.createDefaultModel();
            Dataset left = new DefaultDatasetLoader.ModelDataset(leftModel, "left");

            Model rightModel = ModelFactory.createDefaultModel();
            Dataset right = new DefaultDatasetLoader.ModelDataset(rightModel, "right");


            Map<String, Resource> leftResByDef = new HashMap<>(), rightResByDef = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader(mwsaFile))) {
                String line;
                Map<Pair<String, String>, Set<String>> leftDefns = new HashMap<>(), rightDefns = new HashMap<>();
                while ((line = br.readLine()) != null) {
                    String[] elems = line.split("\t");
                    if (elems.length == 3) {
                        Pair<String, String> key = new Pair<>(elems[0], "");
                        if (!leftDefns.containsKey(key)) {
                            leftDefns.put(key, new HashSet<>());
                        }
                        leftDefns.get(key).add(elems[1]);
                        if (!rightDefns.containsKey(key)) {
                            rightDefns.put(key, new HashSet<>());
                        }
                        rightDefns.get(key).add(elems[2]);
                    } else if (elems.length == 4) {
                        Pair<String, String> key = new Pair<>(elems[0], elems[1]);
                        if (!leftDefns.containsKey(key)) {
                            leftDefns.put(key, new HashSet<>());
                        }
                        leftDefns.get(key).add(elems[2]);
                        if (!rightDefns.containsKey(key)) {
                            rightDefns.put(key, new HashSet<>());
                        }
                        rightDefns.get(key).add(elems[3]);
                    }
                }
                buildMWSADataset(leftModel, leftDefns, leftResByDef);
                buildMWSADataset(rightModel, rightDefns, rightResByDef);
            }
            AlignmentSet set = execute("naisc", left, right, config, new None<>(), monitor, null, null, loader);

            Map<Pair<URIRes, URIRes>, String> results = new HashMap<>();
            for (Alignment a : set) {
                results.put(new Pair<>(a.entity1, a.entity2), a.property);
            }
            try (BufferedReader br = new BufferedReader(new FileReader(mwsaFile))) {
                try (PrintWriter out = new PrintWriter(outputFile)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] elems = line.split("\t");
                        Resource r1 = leftResByDef.get(elems[elems.length - 2] + "\t" + elems[0]);
                        Resource r2 = rightResByDef.get(elems[elems.length - 1] + "\t" + elems[0]);
                        if (r1 == null || r2 == null) {
                            throw new RuntimeException("file changed?");
                        }
                        String result = results.get(new Pair<>(r1, r2));
                        if(result != null) {
                            out.println(line + "\t" + mapResult(result));
                        } else {
                            out.println(line + "\tnone");
                        }
                    }
                }
            }

        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            monitor.message(Stage.FAILED, NaiscListener.Level.CRITICAL, "The process failed due to an exception: " + x.getClass().getName() + ": " + x.getMessage());
        }
    }

    private static String mapResult(String result) {
        if(result.equals(Alignment.SKOS_EXACT_MATCH)) {
            return "exact";
        } else if(result.equals(SKOS.narrowMatch.toString())) {
            return "narrower";
        } else if(result.equals(SKOS.broadMatch.toString())) {
            return "narrower";
        } else if(result.equals(SKOS.relatedMatch.toString())) {
            return "narrower";
        } else {
            System.err.println("Unrecognized property: " + result);
            return "none";
        }
    }

    private static void buildMWSADataset(Model model, Map<Pair<String, String>, Set<String>> defns, Map<String, Resource> resByDef) {
        int i = 0;
        for (Map.Entry<Pair<String, String>, Set<String>> e : defns.entrySet()) {
            for (String defn : e.getValue()) {
                Resource r = model.createResource("entry" + i++);
                if (resByDef.containsKey(defn + "\t" + e.getKey()._1)) {
                    throw new RuntimeException("Duplicate definition: " + defn);
                }
                resByDef.put(defn + "\t" + e.getKey()._1, r);
                r.addLiteral(SKOS.definition, defn);
                r.addLiteral(RDFS.label, e.getKey()._1);
                if (!e.getKey()._2.equalsIgnoreCase("")) {
                    r.addLiteral(model.createProperty("http://www.lexinfo.net/ontology/2.0/lexinfo#partOfSpeech"),
                            model.createResource("http://www.lexinfo.net/ontology/2.0/lexinfo#" + e.getKey()._2));
                }
            }
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
                    accepts("mwsa", "A MWSA file to link").withRequiredArg().ofType(File.class);
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
            final File outputFile = (File) os.valueOf("o");
            final File configuration = (File) os.valueOf("c");
            if (configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist or not specified");
            }
            if (os.hasArgument("mwsa")) {
                executeMWSA((File) os.valueOf("mwsa"), configuration, outputFile, os.valueOf("q") != null && os.valueOf("q").equals(Boolean.TRUE)
                                ? ExecuteListeners.NONE : ExecuteListeners.STDERR,
                        new DefaultDatasetLoader());

            } else {
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

                final Option<File> partialSoln = os.valueOf("p") == null ? new None<>() : new Some<>((File) os.valueOf("p"));
                //final boolean example = os.has("x");
                final boolean outputXML = os.has("xml");
                //final boolean hard = !os.has("easy");
                execute("naisc", left, right, configuration, outputFile,
                        partialSoln, outputXML,
                        os.valueOf("q") != null && os.valueOf("q").equals(Boolean.TRUE)
                                ? ExecuteListeners.NONE : ExecuteListeners.STDERR,
                        new DefaultDatasetLoader());
            }
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

    private static class FilterBlocks extends AbstractCollection<Blocking> {

        private final Collection<Blocking> iter;
        private final Set<URIRes> left, right;

        public FilterBlocks(Collection<Blocking> iter, Set<URIRes> left, Set<URIRes> right) {
            this.iter = iter;
            this.left = left;
            this.right = right;
        }

        @Override
        public Iterator<Blocking> iterator() {
            final Iterator<Blocking> i = iter.iterator();
            return new Iterator<Blocking>() {
                Blocking p = advance();

                @Override
                public boolean hasNext() {
                    return p != null;
                }

                @Override
                public Blocking next() {
                    if (p == null) {
                        throw new NoSuchElementException();
                    }
                    Blocking rval = p;
                    p = advance();
                    return rval;
                }

                private Blocking advance() {
                    while (i.hasNext()) {
                        Blocking x = i.next();
                        if (left.contains(x.entity1) && right.contains(x.entity2)) {
                            return x;
                        }
                    }
                    return null;
                }
            };
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }
    }

    private static AlignmentSet convertAligns(ConcurrentLinkedQueue<TmpAlignment> tmpAligns, Rescaler rescaler) {
        List<Alignment> aligns = new ArrayList<>();
        double[] scores = new double[tmpAligns.size()];
        int i = 0;
        for (TmpAlignment t : tmpAligns) {
            scores[i++] = t.result.getProbability();
        }
        i = 0;
        scores = rescaler.rescale(scores);
        for (TmpAlignment t : tmpAligns) {
            aligns.add(new Alignment(t.left, t.right, scores[i++], t.relation, t.features));
        }
        return new AlignmentSet(aligns);
    }

    private static class TmpAlignment {

        private final URIRes left, right;
        private final ScoreResult result;
        private final String relation;
        private final Object2DoubleMap<String> features;

        public TmpAlignment(URIRes left, URIRes right, ScoreResult result, String relation, FeatureSet featureSet) {
            this.left = left;
            this.right = right;
            this.result = result;
            this.relation = relation;
            if (featureSet != null) {
                this.features = new Object2DoubleOpenHashMap<>();
                for (int i = 0; i < featureSet.names.length; i++) {
                    this.features.put(featureSet.names[i]._1 + "-" + featureSet.names[i]._2, featureSet.values[i]);
                }
            } else {
                this.features = null;
            }
        }
    }
}
