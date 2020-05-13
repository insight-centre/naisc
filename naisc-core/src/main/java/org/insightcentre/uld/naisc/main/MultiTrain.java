package org.insightcentre.uld.naisc.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.DatasetLoader;
import org.insightcentre.uld.naisc.EvaluationSet;
import org.insightcentre.uld.naisc.FeatureSetWithScore;
import org.insightcentre.uld.naisc.NaiscListener;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.NONE;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.STDERR;
import static org.insightcentre.uld.naisc.main.Train.mapper;
import static org.insightcentre.uld.naisc.main.Train.readAlignments;

/**
 * Train on many datasets at once. This is used to train the baseline models
 *
 * @author John McCrae
 */
public class MultiTrain {

    public static void execute(String name, List<EvaluationSet> evaluationSets,
            double negativeSampling,
            File configuration, ExecuteListener monitor,
            DatasetLoader loader, File featureFile) throws IOException {
        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading Configuration");
        final Configuration config = mapper.readValue(configuration, Configuration.class);

        Map<String, List<FeatureSetWithScore>> trainData = new HashMap<>();
        for (EvaluationSet es : evaluationSets) {
            monitor.updateStatus(NaiscListener.Stage.INITIALIZING, "### Dataset:" + es.name() + " ###");
            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading left dataset");
            Dataset leftModel = loader.fromFile(es.left(), name + "/left");
            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading right dataset");
            Dataset rightModel = loader.fromFile(es.right(), name + "/right");
            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading alignments");
            AlignmentSet goldAlignments = readAlignments(es.align().get(), leftModel.id(), rightModel.id());
            Map<String, List<FeatureSetWithScore>> d = Train.extractData(name, leftModel, rightModel, goldAlignments, negativeSampling, config, monitor, loader);
            merge(trainData, d);
        }
        if (featureFile != null) {
            dumpFeatures(featureFile, trainData);
        }

        Train.trainModels(monitor, config, trainData, null);
    }

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
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("q", "Quiet (suppress output)");
                    accepts("f", "Dump features").withRequiredArg().ofType(File.class);
                    accepts("n", "Negative Sampling rate (number of negative examples/positive example)").withRequiredArg().ofType(Double.class);
                    nonOptions("The datasets to use for training");
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
            if (os.nonOptionArguments().isEmpty()) {
                badOptions(p, "Please give at least one dataset to train");
                return;
            }
            List<EvaluationSet> evalSets = new ArrayList<>();
            for (Object evalSetName : os.nonOptionArguments()) {
                EvaluationSet es = new EvaluationSet(new File(new File("datasets"), evalSetName.toString()));
                if (!es.left().exists()) {
                    badOptions(p, es.left().getName() + " does not exist for ");
                    return;
                }
                if (!es.right().exists()) {
                    badOptions(p, es.right().getName() + " does not exist");
                    return;
                }
                if (es.align().has() && es.align().get().exists()) {
                    evalSets.add(es);
                } else if (!os.has("q")) {
                    System.err.println("Skipping " + evalSetName + " as there is no gold standard");
                }
            }
            final File configuration = (File) os.valueOf("c");
            if (configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist or not specified");
            }
            @SuppressWarnings("null")
            final double negativeSampling = os.has("n") ? (Double) os.valueOf("n") : 5.0;
            execute("train", evalSets, negativeSampling, configuration,
                    os.has("q") ? NONE : STDERR,
                    new DefaultDatasetLoader(),
                    (File) os.valueOf("f"));
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

    private static void merge(Map<String, List<FeatureSetWithScore>> trainData, Map<String, List<FeatureSetWithScore>> d) {
        for (Map.Entry<String, List<FeatureSetWithScore>> e : d.entrySet()) {
            if (!trainData.containsKey(e.getKey())) {
                trainData.put(e.getKey(), e.getValue());
            } else {
                trainData.get(e.getKey()).addAll(e.getValue());
            }
        }
    }

    private static void dumpFeatures(File outFile, Map<String, List<FeatureSetWithScore>> trainData) throws IOException {
        boolean first = true;
        try (PrintWriter out = new PrintWriter(outFile)) {
            for (Map.Entry<String, List<FeatureSetWithScore>> e : trainData.entrySet()) {
                for (FeatureSetWithScore fss : e.getValue()) {
                    if(first) {
                        out.print("Property");
                        for (int i = 0; i < fss.names.length; i++) {
                            out.print(",");
                            out.print((fss.names[i]._1+"|||"+ fss.names[i]._2).replaceAll("[^A-Za-z0-9]","_").replaceAll("_+","_"));
                        }
                        out.println(",Score");
                        first = false;
                    }
                    out.print(e.getKey());
                    for (int i = 0; i < fss.names.length; i++) {
                        out.print(",");
                        out.print(fss.values[i]);
                    }
                    out.print(",");
                    out.println(fss.score);
                }
            }
        }
    }
}
