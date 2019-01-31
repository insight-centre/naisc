package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
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
import java.util.Set;
import java.util.regex.Pattern;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RiotException;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.FeatureSetWithScore;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.ScorerTrainer;
import org.insightcentre.uld.naisc.TextFeature;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.GraphFeature;

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

    public static AlignmentSet readAlignments(File alignmentFile) throws IOException {
        AlignmentSet alignments = new AlignmentSet();
        Model model = ModelFactory.createDefaultModel();
        BufferedReader br = new BufferedReader(new FileReader(alignmentFile));
        Pattern lineRegex = Pattern.compile("(<.*>\\s+<.*>\\s+<.*>\\s*\\.)\\s*(#\\s*(\\d*\\.?\\d*))?\\s*");
        String line = br.readLine();
        while (line != null && !line.matches("\\s*")) {
            java.util.regex.Matcher m = lineRegex.matcher(line);
            if(m.matches()) {
                model.removeAll();
                try {
                    model.read(new StringReader(m.group(1)), alignmentFile.toURI().toString(), "NTRIPLES");
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
                alignments.add(new Alignment(st, score));
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
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param alignment The alignments to learn
     * @param configuration The configuration file
     * @throws IOException If an IO error occurs
     */
    public static void execute(File leftFile, File rightFile, File alignment,
            File configuration, ExecuteListener monitor) throws IOException {
        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading Configuration");
        final Configuration config = mapper.readValue(configuration, Configuration.class);
        execute(leftFile, rightFile, alignment, config, monitor);
    }

    /**
     * Execute a NAISC training run
     *
     * @param leftFile The left RDF dataset to align
     * @param rightFile The right RDF dataset to align
     * @param alignment The alignments to learn
     * @param config The configuration object
     * @throws IOException If an IO error occurs
     */
    public static void execute(File leftFile, File rightFile, File alignment,
            Configuration config, ExecuteListener monitor) throws IOException {
        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading left dataset");
        Model leftModel = ModelFactory.createDefaultModel();
        leftModel.read(new FileReader(leftFile), leftFile.toURI().toString(), "riot");

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading right dataset");
        Model rightModel = ModelFactory.createDefaultModel();
        rightModel.read(new FileReader(rightFile), rightFile.toURI().toString(), "riot");

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading alignments");
        AlignmentSet goldAlignments = readAlignments(alignment);
        execute(leftModel, rightModel, goldAlignments, config, monitor);
    }

    /**
     * Train a NAISC model
     *
     * @param leftModel The left dataset
     * @param rightModel The right dataset
     * @param goldAlignments The gold standard alignments
     * @param config The configuration object
     * @param monitor The listener for events
     * @throws IOException If an IO error occurs
     */
    public static void execute(Model leftModel, Model rightModel,
            AlignmentSet goldAlignments,
            Configuration config, ExecuteListener monitor) throws IOException {

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading blocking strategy");
        BlockingStrategy blocking = config.makeBlockingStrategy();

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading lenses");
        Model combined = ModelFactory.createDefaultModel();
        combined.add(leftModel);
        combined.add(rightModel);
        List<Lens> lenses = config.makeLenses(combined);

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading Feature Extractors");
        List<TextFeature> textFeatures = config.makeTextFeatures();
        List<GraphFeature> dataFeatures = config.makeDataFeatures(combined);

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading Scorers");
        List<ScorerTrainer> scorers = config.makeTrainableScorers();

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading Matcher");
        Matcher matcher = config.makeMatcher();

        monitor.updateStatus(ExecuteListener.Stage.BLOCKING, "Blocking");
        final Iterable<Pair<Resource, Resource>> blocks = blocking.block(leftModel, rightModel);

        monitor.updateStatus(ExecuteListener.Stage.TRAINING, "Constructing Training Data");
        Map<String, List<FeatureSetWithScore>> trainingData = new HashMap<>();
        Set<String> goldProps = goldAlignments.properties();
        for (String prop : goldProps) {
            trainingData.put(prop, new ArrayList<>());
        }
        //AlignmentSet alignments = new AlignmentSet();
        for (Pair<Resource, Resource> block : blocks) {
            FeatureSet featureSet = new FeatureSet(block._1, block._2);
            for (Lens lens : lenses) {
                Option<LangStringPair> oFacet = lens.extract(block._1, block._2);
                if (!oFacet.has()) {
                    monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, String.format("Lens produced no label for %s %s", block._1, block._2));
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
            for (String prop : goldProps) {
                Option<Alignment> a = goldAlignments.find(block._1.getURI(), block._2.getURI(), prop);
                if (a.has()) {
                    trainingData.get(prop).add(featureSet.withScore(a.get().score));
                } else {
                    // TODO: Negative Sampling
                }
            }
        }

        monitor.updateStatus(ExecuteListener.Stage.TRAINING, "Learning model");
        //ArrayList<Scorer> trainedScorers = new ArrayList<>();
        for (ScorerTrainer tsf : scorers) {
            List<FeatureSetWithScore> data = trainingData.get(tsf.property());
            if (data != null) {
                tsf.train(data);
            } else {
                System.err.println(String.format("No data for %s so could not train model", tsf.property()));
            }
            tsf.close();
        }
    }

    public static void main(String[] args) {
        try {
            final OptionParser p = new OptionParser() {
                {
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("q", "Quiet (suppress output)");
                    nonOptions("Two RDF files and One Alignment RDF files (N-Triples, one per line with score after as a comment)");
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
            };
            execute(left, right, alignment, configuration,
                    os.has("q") ? new Main.NoMonitor() : new Main.StdErrMonitor());
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }
}
