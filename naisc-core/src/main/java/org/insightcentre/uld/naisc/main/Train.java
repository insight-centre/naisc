package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
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
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
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

    public static  Map<Property, Object2DoubleMap<Statement>>  readAlignments(File alignmentFile) throws IOException {
        Map<Property, Object2DoubleMap<Statement>> alignments = new HashMap<>();
        Model model = ModelFactory.createDefaultModel();
        BufferedReader br = new BufferedReader(new FileReader(alignmentFile));
        String line = br.readLine();
        while (line != null) {
            String[] elems = line.split("#"); // Comment occurs at end of line
            if (elems.length == 2) {
                model.removeAll();
                model.read(new StringReader(elems[0]), alignmentFile.toURI().toString());
                final Statement st;
                try {
                    st = model.listStatements().next();
                } catch (NoSuchElementException x) {
                    throw new IOException("Bad line in alignments (no statement)", x);
                }
                double score = Double.parseDouble(elems[1]);
                if (!alignments.containsKey(st.getPredicate())) {
                    alignments.put(st.getPredicate(), new Object2DoubleOpenHashMap<Statement>());
                }
                alignments.get(st.getPredicate()).put(st, score);
            } else {
                model.removeAll();
                model.read(new StringReader(line), alignmentFile.toURI().toString(), "NTRIPLES");
                final Statement st;
                try {
                    st = model.listStatements().next();
                } catch (NoSuchElementException x) {
                    throw new IOException("Bad line in alignments (no statement)", x);
                }
                double score = 1.0;
                if (!alignments.containsKey(st.getPredicate())) {
                    alignments.put(st.getPredicate(), new Object2DoubleOpenHashMap<Statement>());
                }
                alignments.get(st.getPredicate()).put(st, score);
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
            File configuration) throws IOException {
        System.err.println("Reading left dataset");
        Model leftModel = ModelFactory.createDefaultModel();
        leftModel.read(new FileReader(leftFile), leftFile.toURI().toString(), "riot");

        System.err.println("Reading right dataset");
        Model rightModel = ModelFactory.createDefaultModel();
        rightModel.read(new FileReader(rightFile), rightFile.toURI().toString(), "riot");

        System.err.println("Reading alignments");
        Map<Property, Object2DoubleMap<Statement>> goldAlignments = readAlignments(alignment);

        System.err.println("Reading Configuration");
        final Configuration config = mapper.readValue(configuration, Configuration.class);

        System.err.println("Loading blocking strategy");
        BlockingStrategy blocking = config.makeBlockingStrategy();

        System.err.println("Loading lenses");
        Model combined = ModelFactory.createDefaultModel();
        combined.add(leftModel);
        combined.add(rightModel);
        List<Lens> lenses = config.makeLenses(combined);

        System.err.println("Loading Feature Extractors");
        List<TextFeature> textFeatures = config.makeTextFeatures();
        List<GraphFeature> dataFeatures = config.makeDataFeatures(combined);

        System.err.println("Loading Scorers");
        List<ScorerTrainer> scorers = config.makeTrainableScorers();

        System.err.println("Loading Matcher");
        Matcher matcher = config.makeMatcher();

        System.err.println("Blocking");
        final Iterable<Pair<Resource, Resource>> blocks = blocking.block(leftModel, rightModel);

        System.err.println("Training");
        Map<String, List<FeatureSetWithScore>> trainingData = new HashMap<>();
        for (Property prop : goldAlignments.keySet()) {
            trainingData.put(prop.getURI(), new ArrayList<>());
        }
        //AlignmentSet alignments = new AlignmentSet();
        for (Pair<Resource, Resource> block : blocks) {
            FeatureSet featureSet = new FeatureSet(block._1, block._2);
            for (Lens lens : lenses) {
                Option<LangStringPair> oFacet = lens.extract(block._1, block._2);
                if(!oFacet.has()) {
                    System.err.println(String.format("Lens produced no label for %s %s", block._1, block._2));
                }
                LangStringPair facet = oFacet.getOrElse(EMPTY_LANG_STRING_PAIR);
                for (TextFeature featureExtractor : textFeatures) {
                    if (featureExtractor.tags() == null || lens.tag() == null
                            || featureExtractor.tags().contains(lens.tag())) {
                        double[] features = featureExtractor.extractFeatures(facet);
                        featureSet.add(new FeatureSet(featureExtractor.getFeatureNames(),
                                lens.id(), features, block._1, block._2));
                    }
                }
            }
            for (GraphFeature feature : dataFeatures) {
                double[] features = feature.extractFeatures(block._1, block._2);
                featureSet.add(new FeatureSet(feature.getFeatureNames(), feature.id(), features, block._1, block._2));
            }
            for (Map.Entry<Property, Object2DoubleMap<Statement>> e : goldAlignments.entrySet()) {
                Statement st = combined.createStatement(block._1, e.getKey(), block._2);
                if (e.getValue().containsKey(st)) {
                    trainingData.get(e.getKey().getURI()).add(featureSet.withScore(e.getValue().getDouble(st)));
                } else {
                    // TODO: Negative Sampling
                }
            }
        }

        System.err.println("Training");
        //ArrayList<Scorer> trainedScorers = new ArrayList<>();
        for(ScorerTrainer tsf : scorers) {
            List<FeatureSetWithScore> data = trainingData.get(tsf.property());
            if(data != null) {
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
            execute(left, right, alignment, configuration);
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }
}
