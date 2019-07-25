package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.TextFeature;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import static org.insightcentre.uld.naisc.main.Main.mapper;
import org.insightcentre.uld.naisc.matcher.Prematcher;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Option;

/**
 * Examines a single pair of URIs to see why they were (or were not) linked
 *
 * @author John McCrae
 */
public class ExamineFeature {

    public static FeatureSet examineFeature(String name,
            File leftFile, File rightFile, File configuration,
            String left, String right, ExecuteListener monitor, DatasetLoader loader) {
        try {
            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading Configuration");
            final Configuration config = mapper.readValue(configuration, Configuration.class);

            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading left dataset");
            Dataset leftDataset = loader.fromFile(leftFile, name + "/left");
            Model leftModel = leftDataset.asModel().get();
            Resource res1 = leftModel.createResource(left);
            if (!leftModel.listStatements(res1, null, (RDFNode) null).hasNext()) {
                System.err.printf("%s is not in model\n", res1);
                System.err.println("Entities are:");
                final ResIterator subjIter = leftModel.listSubjects();
                while (subjIter.hasNext()) {
                    System.err.println("  " + subjIter.next());
                }
            }

            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading right dataset");
            Dataset rightDataset = loader.fromFile(rightFile, name + "/right");
            Model rightModel = rightDataset.asModel().get();
            rightModel.read(new FileReader(rightFile), rightFile.toURI().toString(), "riot");
            Resource res2 = rightModel.createResource(right);
            if (!rightModel.listStatements(res2, null, (RDFNode) null).hasNext()) {
                System.err.printf("%s is not in model\n", res2);
            }
            Lazy<Analysis> analysis = Lazy.fromClosure(() -> {
                DatasetAnalyzer analyzer = new DatasetAnalyzer();
                return analyzer.analyseModel(leftModel, rightModel);
            });
            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading lenses");
            Dataset combined = loader.combine(leftDataset, rightDataset, name + "/combined");
            List<Lens> lenses = config.makeLenses(combined, analysis, monitor);

            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading Feature Extractors");
            Lazy<AlignmentSet> prematch = Lazy.fromClosure(() -> new AlignmentSet());
            List<TextFeature> textFeatures = config.makeTextFeatures();
            List<GraphFeature> dataFeatures = config.makeGraphFeatures(combined, analysis, prematch);

            if (res1.getURI() == null || res1.getURI().equals("")
                    || res1.getURI() == null || res1.getURI().equals("")) {
                throw new RuntimeException("Resource with URI");
            }
            FeatureSet featureSet = new FeatureSet(res1, res2);
            for (Lens lens : lenses) {
                Option<LangStringPair> oFacet = lens.extract(res1, res2);
                if (!oFacet.has()) {
                    monitor.updateStatus(ExecuteListener.Stage.SCORING, String.format("Lens produced no label for %s %s", res1, res2));
                } else {
                    monitor.addLensResult(res1, res2, lens.id(), oFacet.get());
                }
                LangStringPair facet = oFacet.getOrElse(EMPTY_LANG_STRING_PAIR);
                for (TextFeature featureExtractor : textFeatures) {
                    if (featureExtractor.tags() == null || lens.tag() == null
                            || featureExtractor.tags().contains(lens.tag())) {
                        double[] features = featureExtractor.extractFeatures(facet);
                        featureSet = featureSet.add(new FeatureSet(featureExtractor.getFeatureNames(),
                                lens.id(), features, res1, res2));
                    }
                }
            }
            for (GraphFeature feature : dataFeatures) {
                double[] features = feature.extractFeatures(res1, res2);
                featureSet = featureSet.add(new FeatureSet(feature.getFeatureNames(), feature.id(), features, res1, res2));
            }
            return featureSet;

        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(ExecuteListener.Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
            return null;
        }
    }

    private static final LangStringPair EMPTY_LANG_STRING_PAIR = new LangStringPair(Language.UNDEFINED, Language.UNDEFINED, "", "");

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    @SuppressWarnings("UseSpecificCatch")
    public static void main(String[] args) {
        try {
            final OptionParser p = new OptionParser() {
                {
                    accepts("o", "The file to write the output dataset to (STDOUT if omitted)").withRequiredArg().ofType(File.class);
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("q", "Suppress output");
                    nonOptions("LEFT_DATASET RIGHT_DATASET LEFT_URL RIGHT_URL");
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
            if (os.nonOptionArguments().size() != 4) {
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

            FeatureSet fs = examineFeature("examine", left, right, configuration, os.nonOptionArguments().get(2).toString(), os.nonOptionArguments().get(3).toString(),
                    os.valueOf("q") != null && os.valueOf("q").equals(Boolean.TRUE)
                    ? ExecuteListeners.NONE : ExecuteListeners.STDERR, new DefaultDatasetLoader());

            ObjectMapper mapper = new ObjectMapper();
            if (outputFile == null) {
                mapper.writeValue(System.out, fs);
            } else {
                mapper.writeValue(outputFile, fs);
            }
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }
}
