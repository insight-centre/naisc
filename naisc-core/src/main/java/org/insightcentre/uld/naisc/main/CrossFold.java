package org.insightcentre.uld.naisc.main;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import static org.insightcentre.uld.naisc.main.Main.mapper;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Perform a cross-fold evaluation of a configuration on a particular dataset.
 *
 * @author John McCrae
 */
public class CrossFold {

    /**
     * Perform a cross-fold validation
     *
     * @param leftFile The left dataset
     * @param rightFile The right dataset
     * @param gold The alignments
     * @param nFolds The number of folds to use
     * @param output The output file or null to write to STDOUT
     * @param configuration The configuration object
     * @param monitor A listener for events in the execution
     */
    @SuppressWarnings("UseSpecificCatch")
    public static void execute(File leftFile, File rightFile, File gold, int nFolds, File output,
            File configuration, ExecuteListener monitor) {

        try {
            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading Configuration");
            final Configuration config = mapper.readValue(configuration, Configuration.class);

            CrossFoldResult cfr = execute(leftFile, rightFile, gold, nFolds, config, monitor);

            final PrintStream out;
            if (output != null) {
                out = new PrintStream(output);
            } else {
                out = System.out;
            }
            cfr.results.write(out);
        } catch (Exception x) {
            x.printStackTrace();
            monitor.updateStatus(ExecuteListener.Stage.FAILED, x.getClass().getName() + ": " + x.getMessage());
        }

    }

    /**
     * Execute a cross-fold validation
     *
     * @param leftFile The left dataset
     * @param rightFile The right dataset
     * @param gold The gold standard alignments
     * @param nFolds The number of folds to use
     * @param config The configuration
     * @param monitor The listener for events
     * @return The alignments and the evaluation scores
     * @throws IOException If a file cannot be read
     */
    public static CrossFoldResult execute(File leftFile, File rightFile,
            File gold,
            int nFolds,
            Configuration config, ExecuteListener monitor) throws IOException {

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading left dataset");
        Model leftModel = ModelFactory.createDefaultModel();
        leftModel.read(new FileReader(leftFile), leftFile.toURI().toString(), "riot");

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading right dataset");
        Model rightModel = ModelFactory.createDefaultModel();
        rightModel.read(new FileReader(rightFile), rightFile.toURI().toString(), "riot");

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading gold standard");
        final AlignmentSet goldAlignments = Train.readAlignments(gold);

        return execute(leftModel, rightModel, goldAlignments, nFolds, config, monitor);

    }

    /**
     * Execute a cross-fold validation
     * @param leftModel The left dataset
     * @param rightModel The right dataset
     * @param goldAlignments The gold standard alignments
     * @param nFolds The number of folds
     * @param config The alignment configuration
     * @param _monitor A listener for events
     * @return The alignment and evaluation score
     * @throws IOException If an error occurs reading a file
     */
    public static CrossFoldResult execute(Model leftModel, Model rightModel,
            AlignmentSet goldAlignments,
            int nFolds,
            Configuration config, ExecuteListener _monitor) throws IOException {

        _monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Creating data folds");
        Folds folds = splitDataset(goldAlignments, nFolds);

        FoldExecuteListener monitor = new FoldExecuteListener(_monitor, nFolds);
        AlignmentSet as = new AlignmentSet();
        for (int i = 0; i < nFolds; i++) {
            monitor.foldNo++;
            Train.execute(leftModel, rightModel, folds.train(goldAlignments, i), config, monitor);
            as.addAll(Main.execute(leftModel, rightModel, config, monitor, folds.leftSplit.get(i), folds.rightSplit.get(i)));
        }
        monitor.foldNo = 0;

        monitor.updateStatus(ExecuteListener.Stage.EVALUATION, "Starting Evaluation");
        Evaluate.EvaluationResults er = Evaluate.evaluate(as, folds.achievableGold(goldAlignments), monitor);
        return new CrossFoldResult(as, er);
    }

    /**
     * The result of a cross fold evaluation
     */
    public static class CrossFoldResult {

        public final AlignmentSet alignments;
        public final Evaluate.EvaluationResults results;

        public CrossFoldResult(AlignmentSet alignments, Evaluate.EvaluationResults results) {
            this.alignments = alignments;
            this.results = results;
        }

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
                    accepts("o", "The file to write the alignments to").withRequiredArg().ofType(File.class);
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("n", "The number of folds").withRequiredArg().ofType(Integer.class);
                    accepts("q", "Quiet (suppress output)");
                    nonOptions("The two RDF files and the gold standard dataset");
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
                badOptions(p, "Wrong number of command line arguments");
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
            final File gold = new File(os.nonOptionArguments().get(2).toString());
            if (!gold.exists()) {
                badOptions(p, gold.getName() + " does not exist");
                return;
            }
            final File outputFile = (File) os.valueOf("o");
            final int nFolds;
            if (os.valueOf("n") == null) {
                nFolds = 10;
            } else {
                nFolds = (Integer) os.valueOf("n");
            }
            if (nFolds <= 1) {
                badOptions(p, "Must have at least 2 folds");
                return;
            }
            final File configuration = (File) os.valueOf("c");
            if (configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist or not specified");
            }
            execute(left, right, gold, nFolds, outputFile, configuration,
                    os.has("q") ? new Main.NoMonitor() : new Main.StdErrMonitor());
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

    /** The maximum number of iterations to use in finding a good split */
    public static final int MAX_ITERS = 10;

    private static List<Pair<String, String>> getAllPairs(AlignmentSet alignments) {
        List<Pair<String, String>> pairs = new ArrayList<>();
        for (Alignment a : alignments) {
            pairs.add(new Pair(a.entity1, a.entity2));
        }
        return pairs;
    }

    /**
     * Split the dataset, randomly but heuristically. This method attempts to
     * group the entities according to
     * <b>precision</b> which is the number of links in the same fold, divided
     * by the total number of links. This method greedily attempts to maximize
     * this.
     *
     * @param alignments The alignment data
     * @param folds The number of folds
     * @return Returns the split of entities on the left (subject) side and
     * right (object) side
     */
    public static Folds splitDataset(AlignmentSet alignments, int folds) {
        final List<Pair<String, String>> allPairs = getAllPairs(alignments);
        final List<String> leftEntities = new ArrayList<>(allPairs.stream().map(x -> x._1).collect(Collectors.toSet()));
        final List<String> rightEntities = new ArrayList<>(allPairs.stream().map(x -> x._2).collect(Collectors.toSet()));

        Collections.shuffle(leftEntities);
        Collections.shuffle(rightEntities);

        final Object2IntMap<String> leftIdx = reverseMap(leftEntities);
        final Object2IntMap<String> rightIdx = reverseMap(rightEntities);

        final IntSet[] leftFolds = new IntSet[folds];
        final IntSet[] rightFolds = new IntSet[folds];
        final IntSet[] forward = new IntSet[leftEntities.size()];
        final IntSet[] backward = new IntSet[rightEntities.size()];
        final int[] byLeftFold = new int[leftEntities.size()];
        final int[] byRightFold = new int[rightEntities.size()];

        for (int i = 0; i < folds; i++) {
            leftFolds[i] = new IntRBTreeSet();
            rightFolds[i] = new IntRBTreeSet();
        }
        for (int i = 0; i < leftEntities.size(); i++) {
            forward[i] = new IntRBTreeSet();
            leftFolds[i % folds].add(i);
            byLeftFold[i] = i % folds;
        }
        for (int i = 0; i < rightEntities.size(); i++) {
            backward[i] = new IntRBTreeSet();
            rightFolds[i % folds].add(i);
            byRightFold[i] = i % folds;
        }

        for (Pair<String, String> p : allPairs) {
            forward[leftIdx.getInt(p._1)].add(rightIdx.getInt(p._2));
            backward[rightIdx.getInt(p._2)].add(leftIdx.getInt(p._1));
        }

        final int[] lsize = new int[folds];
        final int[] rsize = new int[folds];
        int inlinks = 0;
        final int alllinks = allPairs.size();

        for (int i = 0; i < folds; i++) {
            lsize[i] = leftFolds[i].size();
            rsize[i] = rightFolds[i].size();
            for (int l : leftFolds[i]) {
                for (int r : forward[l]) {
                    if (rightFolds[i].contains(r)) {
                        inlinks++;
                    }
                }
            }
        }

        boolean improved = true;
        int iters = 0;

        while (improved && ++iters <= MAX_ITERS) {
            improved = false;
            StepResult res = step(leftEntities, forward, rightFolds, byLeftFold, inlinks, alllinks, leftFolds);
            if (res.improved) {
                improved = true;
                inlinks = res.inlinks;
            }
            res = step(rightEntities, backward, leftFolds, byRightFold, inlinks, alllinks, rightFolds);
            if (res.improved) {
                improved = true;
                inlinks = res.inlinks;
            }
        }

        return new Folds(mapSplit(leftFolds, leftEntities),
                mapSplit(rightFolds, rightEntities));
    }

    private static class StepResult {

        public final int inlinks;
        public final boolean improved;

        public StepResult(int inlinks, boolean improved) {
            this.inlinks = inlinks;
            this.improved = improved;
        }

    }

    private static StepResult step(final List<String> entities, final IntSet[] links,
            final IntSet[] reverseFolds, final int[] byFold,
            int inlinks, final int alllinks,
            final IntSet[] leftFolds) {
        boolean improved = false;
        double precision = (double) inlinks / (double) alllinks;
        for (int l = 0; l < entities.size(); l++) {
            double[] score = new double[entities.size()];
            int[] dinlinks = new int[entities.size()];
            int d = 0;
            for (int t : links[l]) {
                if (reverseFolds[byFold[l]].contains(t)) {
                    d -= 1;
                }
            }
            Arrays.fill(dinlinks, d);
            for (int l2 = 0; l2 < entities.size(); l2++) {
                for (int t : links[l]) {
                    if (reverseFolds[byFold[l2]].contains(t)) {
                        dinlinks[l2] += 1;
                    }
                }
                for (int t : links[l2]) {
                    if (reverseFolds[byFold[l2]].contains(t)) {
                        dinlinks[l2] -= 1;
                    }
                    if (reverseFolds[byFold[l]].contains(t)) {
                        dinlinks[l2] += 1;
                    }
                }
                double precision2 = (double) (inlinks + dinlinks[l2]) / alllinks;
                score[l2] = precision2 - precision;
            }
            int best = whichMax(score);
            if (score[best] > 0) {
                int fold1 = byFold[l];
                int fold2 = byFold[best];
                if (fold1 == fold2) {
                    throw new RuntimeException();
                }
                //System.err.println(String.format("Swapping %d with %d", l, best));
                inlinks += dinlinks[best];
                precision = (double) inlinks / alllinks;
                leftFolds[fold1].remove(l);
                leftFolds[fold1].add(best);
                leftFolds[fold2].add(l);
                leftFolds[fold2].remove(best);
                byFold[l] = fold2;
                byFold[best] = fold1;
                improved = true;
            }
        }
        return new StepResult(inlinks, improved);

    }

    private static List<Set<String>> mapSplit(IntSet[] left, List<String> entities) {
        return Arrays.asList(left).stream().map(x -> x.stream().map(y -> entities.get(y)).collect(Collectors.toSet())).collect(Collectors.toList());
    }

    private static int whichMax(double[] score) {
        int i = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (int j = 0; j < score.length; j++) {
            if (score[j] > max) {
                i = j;
                max = score[j];
            }
        }
        return i;
    }

    private static Object2IntMap<String> reverseMap(List<String> leftEntities) {
        Object2IntMap<String> m = new Object2IntOpenHashMap<>();
        for (int i = 0; i < leftEntities.size(); i++) {
            m.put(leftEntities.get(i), i);
        }
        return m;
    }

    /**
     * The result of a cross-fold split
     */
    public static class Folds {

        /**
         * The left and right split
         */
        public final List<Set<String>> leftSplit, rightSplit;

        public Folds(List<Set<String>> leftSplit, List<Set<String>> rightSplit) {
            this.leftSplit = leftSplit;
            this.rightSplit = rightSplit;
        }

        /**
         * Reduce the alignments to the training set for this run
         *
         * @param alignments The alignments
         * @param foldNo The fold number
         * @return The alignments not using entities in the target fold
         */
        @SuppressWarnings("element-type-mismatch")
        public AlignmentSet train(AlignmentSet alignments, int foldNo) {
            AlignmentSet m = new AlignmentSet();
            for (Alignment a : alignments) {
                if (!leftSplit.get(foldNo).contains(a.entity1)
                        && !rightSplit.get(foldNo).contains(a.entity2)) {
                    m.add(a);
                }
            }
            return m;
        }

        /**
         * Reduce the alignments to the test set for this run
         *
         * @param alignments The alignments
         * @param foldNo The fold number
         * @return The alignments using only entities in the target fold
         */
        @SuppressWarnings("element-type-mismatch")
        public AlignmentSet test(AlignmentSet alignments, int foldNo) {
            AlignmentSet m = new AlignmentSet();
            for (Alignment a : alignments) {
                if (leftSplit.get(foldNo).contains(a.entity1)
                        && rightSplit.get(foldNo).contains(a.entity2)) {
                    m.add(a);
                }
            }
            return m;
        }

        /**
         * Get the percentage of the gold standard that is achievable in this
         * evaluation
         *
         * @param alignments The alignments
         * @return
         */
        @SuppressWarnings("element-type-mismatch")
        public AlignmentSet achievableGold(AlignmentSet alignments) {
            AlignmentSet m = new AlignmentSet();
            for (Alignment a : alignments) {
                for (int foldNo = 0; foldNo < leftSplit.size(); foldNo++) {
                    if (leftSplit.get(foldNo).contains(a.entity1)
                            && rightSplit.get(foldNo).contains(a.entity2)) {
                        m.add(a);
                        break;
                    }
                }
            }
            return m;

        }
    }

    private static class FoldExecuteListener implements ExecuteListener {

        final ExecuteListener mem;
        final int nFolds;
        int foldNo = 0;

        public FoldExecuteListener(ExecuteListener mem, int nFolds) {
            this.mem = mem;
            this.nFolds = nFolds;
        }

        @Override
        public void updateStatus(Stage stage, String message) {
            if (foldNo > 0) {
                mem.updateStatus(stage, String.format("[Fold %d/%d] %s", foldNo, nFolds, message));
            } else {
                mem.updateStatus(stage, message);
            }
        }

        @Override
        public void addLensResult(Resource id1, Resource id2, String lensId, LangStringPair res) {
            mem.addLensResult(id1, id2, lensId, res);
        }

    }
}
