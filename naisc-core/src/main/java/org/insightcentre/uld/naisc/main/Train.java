package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import joptsimple.OptionParser;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RiotException;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import org.insightcentre.uld.naisc.matcher.Prematcher;
import org.insightcentre.uld.naisc.util.Lazy;
import org.jetbrains.annotations.Nullable;

/**
 *
 * @author John McCrae
 */
public class Train {

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
    final static ObjectMapper mapper = new ObjectMapper();

    public static AlignmentSet readAlignments(File alignmentFile, String leftDataset, String rightDataset) throws IOException {
        AlignmentSet alignments = new AlignmentSet();
        Model model = ModelFactory.createDefaultModel();
        BufferedReader br = new BufferedReader(new FileReader(alignmentFile));
        Pattern lineRegex = Pattern.compile("(<.*>\\s+<.*>\\s+<.*>\\s*\\.)\\s*(#\\s*(\\d*\\.?\\d*))?\\s*");
        String line = br.readLine();
        while (line != null && !line.matches("\\s*")) {
            java.util.regex.Matcher m = lineRegex.matcher(line);
            if (m.matches()) {
                model.removeAll();
                try {
                    model.read(new StringReader(m.group(1)), alignmentFile.toURI().toString(), "N-TRIPLES");
                } catch (RiotException x) {
                    throw new RuntimeException("Could not read line: " + line, x);
                }
                final Statement st;
                try {
                    st = model.listStatements().next();
                } catch (NoSuchElementException x) {
                    throw new IOException("Bad line in alignments (no statement)", x);
                }
                double score = m.group(2) == null ? 1.0 : Double.parseDouble(m.group(3));
                alignments.add(new Alignment(
                    new URIRes(st.getSubject().getURI(), leftDataset),
                    new URIRes(st.getObject().asResource().getURI(), rightDataset),
                    score, st.getPredicate().getURI(), null));
            } else {
                throw new RuntimeException("Line does not seem valid: " + line);
            }
            line = br.readLine();
        }
        return alignments;
    }
//    
//    public static AlignmentSet readAlignmentSet(File alignmentFile) throws IOException {
//         Map<Property, Object2DoubleMap<Statement>> alignments = readAlignments(alignmentFile);
//        AlignmentSet aligns = new AlignmentSet();
//        for(Map.Entry<Property, Object2DoubleMap<Statement>> e : alignments.entrySet()) {
//            for(Object2DoubleMap.Entry<Statement> e2 : e.getValue().object2DoubleEntrySet()) {
//                Alignment a = new Alignment(e2.getKey().getSubject(), e2.getKey().getObject().asResource(), e2.getDoubleValue(), e.getKey().getURI());
//                aligns.add(a);
//            }
//        }
//        return aligns;
//    }

    private static final LangStringPair EMPTY_LANG_STRING_PAIR = new LangStringPair(Language.UNDEFINED, Language.UNDEFINED, "", "");

    /**
     * Execute NAISC
     *
     * @param name The identifier for this run
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param alignment The alignments to learn
     * @param negativeSampling The rate at which to generate negative examples
     * @param configuration The configuration file
     * @param monitor The listener for events
     * @param loader The dataset loader
     * @param tag The tag (for cross-fold validation)
     * @throws IOException If an IO error occurs
     */
    public static void execute(String name, File leftFile, File rightFile, File alignment,
            double negativeSampling,
            File configuration, ExecuteListener monitor,
            DatasetLoader loader, @Nullable String tag) throws IOException {
        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading Configuration");
        final Configuration config = mapper.readValue(configuration, Configuration.class);
        execute(name, leftFile, rightFile, alignment, negativeSampling, config, monitor, loader, tag);
    }

    /**
     * Execute a NAISC training run
     *
     * @param name The identifier for this run
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param alignment The alignments to learn
     * @param negativeSampling The rate at which to generate negative examples
     * @param config The configuration object
     * @param monitor The listener for eventsDataset
     * @param loader The dataset loader
     * @param tag The tag (for cross-fold validation)
     * @throws IOException If an IO error occurs
     */
    public static void execute(String name, File leftFile, File rightFile, File alignment,
            double negativeSampling,
            Configuration config, ExecuteListener monitor, DatasetLoader loader,
            @Nullable String tag) throws IOException {
        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading alignments");
        AlignmentSet goldAlignments = readAlignments(alignment, leftFile.getName(), rightFile.getName());
        execute(name, leftFile, rightFile, goldAlignments, negativeSampling, config, monitor, loader, tag);
    }

    /**
     * Execute a NAISC training run
     *
     * @param name The identifier for this run
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param goldAlignments The alignments to learn
     * @param negativeSampling The rate at which to generate negative examples
     * @param config The configuration object
     * @param monitor The listener for eventsDataset
     * @param loader The dataset loader
     * @param tag The tag (for cross-fold validation)
     * @throws IOException If an IO error occurs
     */
    public static void execute(String name, File leftFile, File rightFile, AlignmentSet goldAlignments,
            double negativeSampling,
            Configuration config, ExecuteListener monitor, DatasetLoader loader,
            @Nullable String tag) throws IOException {
        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading left dataset");
        Dataset leftModel = loader.fromFile(leftFile, name + "/left");

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading right dataset");
        Dataset rightModel = loader.fromFile(rightFile, name + "/right");
        execute(name, leftModel, rightModel, goldAlignments, negativeSampling, config, monitor, loader, tag);
    }

    /**
     * Train a NAISC model
     *
     * @param name The name of the run
     * @param leftModel The left dataset
     * @param rightModel The right dataset
     * @param goldAlignments The gold standard alignments
     * @param config The configuration object
     * @param negativeSampling The rate at which to generate negative examples
     * (zero for no negative sampling)
     * @param monitor The listener for events
     * @param loader The dataset loader
     * @param tag The tag (for cross-fold validation)
     * @throws IOException If an IO error occurs
     */
    public static void execute(String name, Dataset leftModel, Dataset rightModel,
            AlignmentSet goldAlignments, double negativeSampling,
            Configuration config, ExecuteListener monitor, DatasetLoader loader, @Nullable String tag) throws IOException {
        Map<String, List<FeatureSetWithScore>> trainingData
                = extractData(name, leftModel, rightModel, goldAlignments, negativeSampling, config, monitor, loader);

        trainModels(monitor, config, trainingData, tag);
    }

    /**
     * Extract the data to be trained
     *
     * @param name The name of the run
     * @param leftModel The left dataset
     * @param rightModel The right dataset
     * @param goldAlignments The gold standard
     * @param negativeSampling The negative sampling rate
     * @param config The configuration
     * @param monitor The monitor
     * @param loader The dataset loader
     * @return The training data
     * @throws IOException If a disk error occurred
     */
    public static Map<String, List<FeatureSetWithScore>> extractData(String name, Dataset leftModel, Dataset rightModel,
            AlignmentSet goldAlignments, double negativeSampling,
            Configuration config, ExecuteListener monitor, DatasetLoader loader) throws IOException {
        Map<String, List<FeatureSetWithScore>> trainingData = new HashMap<>();
        Lazy<Analysis> analysis = Lazy.fromClosure(() -> {
            DatasetAnalyzer analyzer = new DatasetAnalyzer();
            return analyzer.analyseModel(leftModel,
                    rightModel);
        });
        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading blocking strategy");
        BlockingStrategy blocking = config.makeBlockingStrategy(analysis, monitor);

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading lenses");
        Dataset combined = loader.combine(leftModel, rightModel, name + "/combined");
        List<Lens> lenses = config.makeLenses(combined, analysis, monitor);

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading Feature Extractors");
        AlignmentSet prematch = new Prematcher().prematch(blocking.block(leftModel, rightModel), leftModel, rightModel);
        List<TextFeature> textFeatures = config.makeTextFeatures();
        List<GraphFeature> dataFeatures = config.makeGraphFeatures(combined, analysis, prematch, monitor);

        monitor.updateStatus(ExecuteListener.Stage.BLOCKING, "Blocking");
        final Iterable<Blocking> blocks = blocking.block(leftModel, rightModel);

        monitor.updateStatus(ExecuteListener.Stage.TRAINING, "Constructing Training Data");
        Set<String> goldProps = goldAlignments.properties();
        if (goldProps.isEmpty()) {
            monitor.updateStatus(ExecuteListener.Stage.FAILED, "No properties in the gold set. Is the training data empty?");
            return new HashMap<>();
        }
        for (String prop : goldProps) {
            trainingData.put(prop, new ArrayList<>());
        }

        boolean blocksGenerated = false;

        // We use the negative sampling as the seed such that the experiment
        // produces the same result every time
        final Random random = new Random(Double.doubleToLongBits(negativeSampling));
        final int estimatedSize = blocking.estimateSize(leftModel, rightModel);
        final double negSampProp = negativeSampling > 0
                ? negativeSampling * (double) (goldAlignments.size()) / (double) (estimatedSize - goldAlignments.size())
                : -1.0;
        final Object2IntMap<String> positives = new Object2IntOpenHashMap<>(),
                negatives = new Object2IntOpenHashMap<>();
        int count = 0;
        for (Blocking block : blocks) {
            blocksGenerated = true;
            if (++count % 10000 == 0) {
                monitor.updateStatus(ExecuteListener.Stage.SCORING,
                        String.format("Generating Features (%.2f%%)", (double) count / estimatedSize * 100.0));
            }
            for (String prop : goldProps) {
                Resource block1 = block.asJena1(leftModel), block2 = block.asJena2(rightModel);
                Option<Alignment> a = goldAlignments.find(block.entity1, block.entity2, prop);

                if (a.has()) {
                    FeatureSet featureSet = makeFeatures(block.entity1, block.entity2, lenses, monitor, textFeatures, dataFeatures, leftModel, rightModel);
                    trainingData.get(prop).add(featureSet.withScore(a.get().probability));
                    goldAlignments.remove(a.get());
                    positives.put(prop, positives.getInt(prop) + 1);
                } else {
                    if (negativeSampling > 0 && random.nextDouble() < negSampProp) {
                        FeatureSet featureSet = makeFeatures(block.entity1, block.entity2, lenses, monitor, textFeatures, dataFeatures, leftModel, rightModel);
                        trainingData.get(prop).add(featureSet.withScore(0.0));
                        negatives.put(prop, negatives.getInt(prop) + 1);
                    }
                }
            }
        }

        int unblockedGold = 0;
        for (Alignment a : goldAlignments) {
            FeatureSet featureSet = makeFeatures(a.entity1,
                    a.entity2, lenses, monitor, textFeatures, dataFeatures, leftModel, rightModel);
            trainingData.get(a.property).add(featureSet.withScore(a.probability));
            unblockedGold++;
        }

        for (String prop : goldProps) {
            if (negativeSampling > 0) {
                monitor.updateStatus(ExecuteListener.Stage.TRAINING, "Adding " + negatives.getInt(prop)
                        + " negative examples to " + positives.getInt(prop)
                        + " positive examples for property " + prop);
            } else {
                monitor.updateStatus(ExecuteListener.Stage.TRAINING, "Generated data for "
                        + positives.getInt(prop) + " examples");
            }
        }

        if (!blocksGenerated) {
            monitor.updateStatus(ExecuteListener.Stage.FAILED, "No blocks were generated");
            return new HashMap<>();
        }
        if (unblockedGold > 0) {
            monitor.updateStatus(ExecuteListener.Stage.TRAINING, unblockedGold + " gold standard values were not generated by the blocking strategy. If this value is high consider using a more exhaustive blocking method");
        }
        return trainingData;

    }

    /**
     * Train the models
     *
     * @param monitor A logger
     * @param config The configuration
     * @param trainingData The training data
     * @param tag The tag (for a cross-fold validation) or null
     * @throws IOException If a disk error occurs
     */
    public static void trainModels(ExecuteListener monitor, Configuration config, Map<String, List<FeatureSetWithScore>> trainingData, @Nullable String tag) throws IOException {
        for (String prop : trainingData.keySet()) {

            monitor.updateStatus(ExecuteListener.Stage.TRAINING, "Loading Scorers");
            List<ScorerTrainer> scorers = config.makeTrainableScorers(prop, tag);

            //ArrayList<Scorer> trainedScorers = new ArrayList<>();
            for (ScorerTrainer tsf : scorers) {
                List<FeatureSetWithScore> data = trainingData.get(prop);
                if (data != null) {
                    monitor.updateStatus(ExecuteListener.Stage.TRAINING, "Learning model (" + data.size() + " items)");
                    tsf.save(tsf.train(data, monitor));
                } else {
                    System.err.println(String.format("No data for %s so could not train model", prop));
                }
                tsf.close();
            }
        }
    }

    private static FeatureSet makeFeatures(URIRes res1, URIRes res2, List<Lens> lenses, ExecuteListener monitor, List<TextFeature> textFeatures, List<GraphFeature> dataFeatures, Dataset left, Dataset right) {
        FeatureSet featureSet = new FeatureSet();
        boolean labelsProduced = false;
        for (Lens lens : lenses) {
            for(LensResult facet : lens.extract(res1, res2)) {
                labelsProduced = true;
                for (TextFeature featureExtractor : textFeatures) {
                    if (featureExtractor.tags() == null || facet.tag == null
                            || featureExtractor.tags().contains(facet.tag)) {
                        Feature[] features = featureExtractor.extractFeatures(facet);
                        featureSet = featureSet.add(new FeatureSet(features,
                                facet.tag));
                    }
                }
            }
        }
        if (!labelsProduced) {
            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, String.format("Lens produced no label for %s %s", res1, res2));
        }
        for (GraphFeature feature : dataFeatures) {
            Feature[] features = feature.extractFeatures(res1, res2);
            featureSet = featureSet.add(new FeatureSet(features, feature.id()));
        }
        return featureSet;
    }
    /*
    @SuppressWarnings("UseSpecificCatch")
    public static void main(String[] args) {
        try {
            final OptionParser p = new OptionParser() {
                {
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("q", "Quiet (suppress output)");
                    accepts("n", "Negative Sampling rate (number of negative examples/positive example)").withRequiredArg().ofType(Double.class);
                    nonOptions("Two RDF files and One Alignment RDF files (N-Triples, one per line with probability after as a comment)");
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
            if (os.nonOptionArguments().size() != 3) {
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
            final File alignment = new File(os.nonOptionArguments().get(2).toString());
            if (!alignment.exists()) {
                badOptions(p, alignment.getName() + " does not exist");
                return;
            }
            final File configuration = (File) os.valueOf("c");
            if (configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist or not specified");
            }
            @SuppressWarnings("null")
            final double negativeSampling = os.has("n") ? (Double) os.valueOf("n") : 5.0;
            execute("train", left, right, alignment, negativeSampling, configuration,
                    os.has("q") ? NONE : STDERR,
                    new DefaultDatasetLoader());
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }*/
}
