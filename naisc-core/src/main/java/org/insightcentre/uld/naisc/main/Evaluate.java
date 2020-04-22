package org.insightcentre.uld.naisc.main;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.Alignment.Valid;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.NaiscListener.Stage;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.NONE;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.STDERR;

import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Tools for evaluating an alignment relative to a gold standard evaluation
 *
 * @author John McCrae
 */
public class Evaluate {

    public static class EvaluationResults {

        public int tp;
        public int fp;
        public int fn;
        public double correlation;
        public List<Pair<Double, EvaluationResults>> thresholds = new ArrayList<>();

        public double precision() {
            return tp + fp > 0 ? (double) tp / (double) (tp + fp) : 1.0;
        }

        public double recall() {
            return tp + fn > 0 ? (double) tp / (double) (tp + fn) : 0.0;
        }

        public double fmeasure() {
            return tp + fp + fn > 0 ? 2.0 * tp / (double) (2.0 * tp + fp + fn) : 0.0;
        }

        public void write(PrintStream out) {
            out.println("Results");
            out.println("-------");
            out.println();
            out.printf("Precision   : %.4f (%d)\n", precision(), tp + fp);
            out.printf("Recall      : %.4f (%d)\n", recall(), tp + fn);
            out.printf("F-Measure   : %.4f\n", fmeasure());
            if (Double.isFinite(correlation)) {
                out.printf("Correlation : %.4f\n", correlation);
            }
            out.println();
            out.println("| Threshold  | Precision  | Recall     | F-Measure  | Predictions |");
            out.println("|------------|------------|------------|------------|-------------|");
            for (Pair<Double, Evaluate.EvaluationResults> threshold : thresholds) {
                out.printf("|  >= %3d%%   |   %.4f   |   %.4f   |   %.4f   | %11d |\n",
                        (int) (threshold._1 * 100),
                        threshold._2.precision(),
                        threshold._2.recall(),
                        threshold._2.fmeasure(),
                        threshold._2.tp + threshold._2.fp);
            }
        }
    }

    public static void evaluate(File testFile, File goldFile, File outputFile, ExecuteListener monitor, boolean ignoreNotInGold, String leftDatatset, String rightDataset) throws IOException {
        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading output alignments");
        final AlignmentSet output = Train.readAlignments(testFile, leftDatatset, rightDataset);

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading gold standard");
        final AlignmentSet goldAlignments = Train.readAlignments(goldFile, leftDatatset, rightDataset);

        EvaluationResults er = evaluate(output, goldAlignments, monitor, ignoreNotInGold);

        final PrintStream out;
        if (outputFile != null) {
            out = new PrintStream(outputFile);
        } else {
            out = System.out;
        }
        er.write(out);
    }

    public static EvaluationResults evaluate(AlignmentSet output,
            AlignmentSet gold, ExecuteListener monitor, boolean ignoreNotInGold) {
        PearsonsCorrelation correlation = new PearsonsCorrelation();
        EvaluationResults er = new EvaluationResults();
        for (int i = 0; i <= 10; i++) {
            er.thresholds.add(new Pair(0.1 * i, new EvaluationResults()));
        }
        Set<URIRes> goldLefts = null, goldRights = null;
        if(ignoreNotInGold) {
            goldLefts = gold.stream().map(a -> a.entity1).collect(Collectors.toSet());
            goldRights = gold.stream().map(a -> a.entity2).collect(Collectors.toSet());
        }

        Set<Alignment> seen = new HashSet<>();
        DoubleList outputScores = new DoubleArrayList(), goldScores = new DoubleArrayList();
        for (Alignment align : output) {
            if (align.valid != Valid.unknown) {
                continue;
            }
            Option<Alignment> galign = gold.find(align.entity1, align.entity2, align.property);
            if (galign.has()) {
                double gScore = galign.get().probability;
                outputScores.add(align.probability);
                goldScores.add(gScore);
                if (gScore > 0 && align.probability > 0) {
                    er.tp++;
                } else if (gScore <= 0 && align.probability > 0) {
                    if(!ignoreNotInGold || goldLefts.contains(align.entity1)  || goldRights.contains(align.entity2)) {
                        er.fp++;
                    }
                }
                for (int i = 0; 0.1 * i <= align.probability && 0.1 * i <= 1.0; i++) {
                    if (gScore > 0 && align.probability > 0) {
                        er.thresholds.get(i)._2.tp++;
                    } else if (gScore <= 0) {
                        er.thresholds.get(i)._2.fp++;
                    }
                }
                if (gScore > 0 && align.probability > 0) {
                    align.valid = Valid.yes;
                } else {
                    if(gold.hasLink(align.entity1, align.entity2)) {
                        align.valid = Valid.bad_link;
                    } else {
                        align.valid = Valid.no;
                    }
                }
                seen.add(galign.get());
            } else {
                if (align.probability > 0) {
                    if(!ignoreNotInGold || goldLefts.contains(align.entity1)  || goldRights.contains(align.entity2)) {
                        er.fp++;
                    }
                }
                for (int i = 0; 0.1 * i <= align.probability; i++) {
                    if (align.probability > 0) {
                        er.thresholds.get(i)._2.fp++;
                    }
                }
                if (align.probability > 0) {
                    if(gold.hasLink(align.entity1, align.entity2)) {
                        align.valid = Valid.bad_link;
                    } else {
                        align.valid = Valid.no;
                    }
                } else {
                    align.valid = Valid.yes;
                }
            }
        }
        if (er.tp == 0) { // Debug message 
            monitor.updateStatus(Stage.EVALUATION, String.format("No true positives! %d in output, %d in gold", output.size(), gold.size()));
            if (!output.isEmpty() && !gold.isEmpty()) {
                StringBuilder sb = new StringBuilder("First few results from output and gold\n\nOutput:\n");
                int i = 0;
                for (Alignment a : output) {
                    sb.append(a).append("\n");
                    if (i++ > 5) {
                        break;
                    }
                }
                i = 0;
                sb.append("\nGold:\n");
                for (Alignment a : gold) {
                    sb.append(a).append("\n");
                    if (i++ > 5) {
                        break;
                    }
                }
                monitor.updateStatus(Stage.EVALUATION, sb.toString());
            }
        }
        int goldSize = 0;
        for (Alignment a : gold.getAlignments()) {
            if (a.probability > 0) {
                goldSize++;
            }
            try {
                if (!seen.contains(a)) {
                    output.add(new Alignment(a, a.probability, Valid.novel));
                }
            } catch (UnsupportedOperationException x) {
                // The alignments cannot be expanded 
            }
        }
        er.fn = goldSize - er.tp;
        for (int i = 0; i <= 10; i++) {
            er.thresholds.get(i)._2.fn = goldSize - er.thresholds.get(i)._2.tp;
        }
        if (goldScores.size() >= 2) {
            er.correlation = correlation.correlation(goldScores.toDoubleArray(), outputScores.toDoubleArray());
        } else {
            er.correlation = 0;
        }
        return er;
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
                    accepts("q", "Quiet (suppress output)");
                    accepts("o", "Output file (or none for STDOUT)");
                    accepts("i", "Ignore URIs not in the gold alignment set");
                    nonOptions("The test alignment and the gold alignment");
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
                badOptions(p, "Wrong number of command line arguments");
                return;
            }
            final File test = new File(os.nonOptionArguments().get(0).toString());
            if (!test.exists()) {
                badOptions(p, test.getName() + " does not exist");
                return;
            }

            final File gold = new File(os.nonOptionArguments().get(1).toString());
            if (!gold.exists()) {
                badOptions(p, gold.getName() + " does not exist");
                return;
            }

            final File outputFile = (File) os.valueOf("o");

            Evaluate.evaluate(test, gold, outputFile,
                    os.has("q") ? NONE : STDERR, os.has("i"), "left", "right");

        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

}
