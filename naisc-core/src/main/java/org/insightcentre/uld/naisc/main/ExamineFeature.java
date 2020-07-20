package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import static org.insightcentre.uld.naisc.main.Main.mapper;

import org.insightcentre.uld.naisc.lens.URI;
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
            Dataset leftDataset = loader.fromFile(leftFile, "left");
            Resource res1 = leftDataset.createResource(left);
            if (!leftDataset.listStatements(res1, null, (RDFNode) null).hasNext()) {
                System.err.printf("%s is not in model\n", res1);
                System.err.println("Entities are:");
                final ResIterator subjIter = leftDataset.listSubjects();
                while (subjIter.hasNext()) {
                    System.err.println("  " + subjIter.next());
                }
            }

            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading right dataset");
            Dataset rightDataset = loader.fromFile(rightFile, "right");
            //rightDataset.read(new FileReader(rightFile), rightFile.toURI().toString(), "riot");
            Resource res2 = rightDataset.createResource(right);
            if (!rightDataset.listStatements(res2, null, (RDFNode) null).hasNext()) {
                System.err.printf("%s is not in model\n", res2);
            }
            Lazy<Analysis> analysis = Lazy.fromClosure(() -> {
                DatasetAnalyzer analyzer = new DatasetAnalyzer();
                return analyzer.analyseModel(leftDataset, rightDataset);
            });
            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading lenses");
            Dataset combined = loader.combine(leftDataset, rightDataset, name + "/combined");
            List<Lens> lenses = config.makeLenses(combined, analysis, monitor);

            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Loading Feature Extractors");
            AlignmentSet prematch = new AlignmentSet();
            List<TextFeature> textFeatures = config.makeTextFeatures();
            List<GraphFeature> dataFeatures = config.makeGraphFeatures(combined, analysis, prematch, monitor);

            if (res1.getURI() == null || res1.getURI().equals("")
                    || res1.getURI() == null || res1.getURI().equals("")) {
                throw new RuntimeException("URIRes with URI");
            }
            FeatureSet featureSet = new FeatureSet();
            for (Lens lens : lenses) {
                Collection<LensResult> facets = lens.extract(URIRes.fromJena(res1, leftDataset.id()),
                    URIRes.fromJena(res2, rightDataset.id()));
                if (facets.isEmpty()) {
                    monitor.updateStatus(ExecuteListener.Stage.SCORING, String.format("Lens produced no label for %s %s", res1, res2));
                }
                for(LensResult facet : facets) {
                    monitor.addLensResult(new URIRes(res1.getURI(), leftDataset.id()), new URIRes(res2.getURI(), rightDataset.id()), facet.tag, facet);
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
            for (GraphFeature feature : dataFeatures) {
                Feature[] features = feature.extractFeatures(URIRes.fromJena(res1, leftDataset.id()), URIRes.fromJena(res2, rightDataset.id()));
                featureSet = featureSet.add(new FeatureSet(features, feature.id()));
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
