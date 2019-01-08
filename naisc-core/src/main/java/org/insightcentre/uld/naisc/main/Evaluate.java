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
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.Alignment.Valid;
import org.insightcentre.uld.naisc.AlignmentSet;
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

    public static void evaluate(File testFile, File goldFile, File outputFile, ExecuteListener monitor) throws IOException {
        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading output alignments");
        final AlignmentSet output = Train.readAlignments(testFile);

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading gold standard");
        final AlignmentSet goldAlignments = Train.readAlignments(goldFile);

        EvaluationResults er = evaluate(output, goldAlignments, monitor);

        final PrintStream out;
        if(outputFile != null) {
            out = new PrintStream(outputFile);
        } else {
            out = System.out;
        }
        er.write(out);
    }

    public static EvaluationResults evaluate(AlignmentSet output,
            AlignmentSet gold, ExecuteListener monitor) {
        PearsonsCorrelation correlation = new PearsonsCorrelation();
        EvaluationResults er = new EvaluationResults();
        for (int i = 0; i <= 10; i++) {
            er.thresholds.add(new Pair(0.1 * i, new EvaluationResults()));
        }
        Set<Alignment> seen = new HashSet<>();
        DoubleList outputScores = new DoubleArrayList(), goldScores = new DoubleArrayList();
        for (Alignment align : output) {
            if (align.valid != Valid.unknown) {
                continue;
            }
            Option<Alignment> galign = gold.find(align.entity1, align.entity2, align.relation);
            if (galign.has()) {
                double gScore = galign.get().score;
                outputScores.add(align.score);
                goldScores.add(gScore);
                er.tp++;
                for (int i = 0; 0.1 * i <= align.score; i++) {
                    er.thresholds.get(i)._2.tp++;
                }
                align.valid = Valid.yes;
                seen.add(galign.get());
            } else {
                er.fp++;
                for (int i = 0; 0.1 * i <= align.score; i++) {
                    er.thresholds.get(i)._2.fp++;
                }
                align.valid = Valid.no;
            }
        }
        int goldSize = 0;
        for (Alignment a : gold.getAlignments()) {
            goldSize++;
            try {
                if (!seen.contains(a)) {
                    output.add(new Alignment(a, a.score, Valid.novel));
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
            
            final File outputFile = (File)os.valueOf("o");
            
            Evaluate.evaluate(test, gold, outputFile,
                    os.has("q") ? new Main.NoMonitor() : new Main.StdErrMonitor());

        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

}
