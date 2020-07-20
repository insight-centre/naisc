package org.insightcentre.uld.naisc.main;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;

import static org.insightcentre.uld.naisc.main.ExecuteListeners.NONE;
import static org.insightcentre.uld.naisc.main.ExecuteListeners.STDERR;
import static org.insightcentre.uld.naisc.main.Main.mapper;

import org.insightcentre.uld.naisc.scorer.ModelNotTrainedException;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Perform a cross-fold evaluation of a configuration on a particular dataset.
 *
 * @author John McCrae
 */
public class CrossFold {
    /**
     * The direction to do the folding.
     */
    public static enum FoldDirection {
        /**
         * Divide the left dataset into n folds. The left elements can be matched to any element in the right set
         */
        left,
        /**
         * Divide the right dataset into n folds.
         */
        right,
        /**
         * Divide each dataset into n folds. The elements can only be matched to elements in the same fold; this generally
         * leads to an overestimation of precision/recall.
         */
        both
    }

    /**
     * Perform a cross-fold validation
     *
     * @param leftFile The left dataset
     * @param rightFile The right dataset
     * @param gold The alignments
     * @param name The name of the run
     * @param nFolds The number of folds to use
     * @param negativeSampling The rate of negative sampling
     * @param output The output file or null to write to STDOUT
     * @param configuration The configuration object
     * @param monitor A listener for events in the execution
     * @param loader Dataset loader
     */
    @SuppressWarnings("UseSpecificCatch")
    public static void execute(String name, File leftFile, File rightFile, File gold, 
            int nFolds, FoldDirection direction, double negativeSampling, File output,
            File configuration, ExecuteListener monitor, DatasetLoader loader) {

        try {
            monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading Configuration");
            final Configuration config = mapper.readValue(configuration, Configuration.class);

            CrossFoldResult cfr = execute(name, leftFile, rightFile, gold, nFolds, direction, negativeSampling, config, monitor, loader);

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
     * @param name The name of this run
     * @param nFolds The number of folds to use
     * @param negativeSampling The rate of negative sampling
     * @param config The configuration
     * @param monitor The listener for events
     * @param loader The dataset loader
     * @return The alignments and the evaluation scores
     * @throws IOException If a file cannot be read
     */
    public static CrossFoldResult execute(String name, File leftFile, File rightFile,
            File gold,
            int nFolds, FoldDirection direction, double negativeSampling,
            Configuration config, ExecuteListener monitor,
            DatasetLoader loader) throws IOException, ModelNotTrainedException {

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading left dataset");
        Dataset leftModel = loader.fromFile(leftFile, "left");

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading right dataset");
        Dataset rightModel = loader.fromFile(rightFile, "right");

        monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Reading gold standard");
        final AlignmentSet goldAlignments = Train.readAlignments(gold, "left", "right");


        return execute(name, leftModel, rightModel, goldAlignments, nFolds, direction, negativeSampling, config, monitor, loader);

    }

    /**
     * Execute a cross-fold validation
     * @param name The name of the run
     * @param leftModel The left dataset
     * @param rightModel The right dataset
     * @param goldAlignments The gold standard alignments
     * @param nFolds The number of folds
     * @param negativeSampling The rate of negative sampling
     * @param config The alignment configuration
     * @param _monitor A listener for events
     * @param loader The dataset loader
     * @return The alignment and evaluation probability
     * @throws IOException If an error occurs reading a file
     */
    public static CrossFoldResult execute(String name, Dataset leftModel, Dataset rightModel,
            AlignmentSet goldAlignments,
            int nFolds, FoldDirection direction, double negativeSampling,
            Configuration config, ExecuteListener _monitor, DatasetLoader loader) throws IOException, ModelNotTrainedException {

        _monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Creating data folds");
        Folds folds = splitDataset(goldAlignments, nFolds, direction);
        _monitor.updateStatus(ExecuteListener.Stage.INITIALIZING, "Folds created: " + folds.toString());

        FoldExecuteListener monitor = new FoldExecuteListener(_monitor, nFolds);
        AlignmentSet as = new AlignmentSet();
        for (int i = 0; i < nFolds; i++) {
            monitor.foldNo++;
            Train.execute(name, leftModel, rightModel, folds.train(goldAlignments, i), negativeSampling, config, monitor,loader, "fold" + i);
            AlignmentSet predicted = Main.execute(name, leftModel, rightModel, config, new None<>(), monitor, folds.leftSplit.get(i), folds.rightSplit.get(i),loader);
            as.addAll(predicted);
            Evaluate.EvaluationResults er = Evaluate.evaluate(predicted, goldAlignments, monitor, true);
            monitor.message(NaiscListener.Stage.TRAINING, NaiscListener.Level.INFO, String.format("Fold results (%d aligns):  precision = %.04f, recall = %.04f, fmeasure = %.04f", predicted.size(), er.precision(), er.recall(), er.fmeasure()));
            for(Alignment a : predicted) {
                a.valid = Alignment.Valid.unknown;
            }
        }
        monitor.foldNo = 0;

        monitor.updateStatus(ExecuteListener.Stage.EVALUATION, "Starting Evaluation");
        Evaluate.EvaluationResults er = Evaluate.evaluate(as, folds.achievableGold(goldAlignments), monitor, false);
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
                    accepts("s", "The negative sampling rate").withRequiredArg().ofType(Double.class);
                    accepts("d", "The direction of the folding (left|right|both)").withRequiredArg().ofType(String.class);
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
            final FoldDirection direction = FoldDirection.valueOf(os.valueOf("d") == null ? "left" : (String)os.valueOf("d"));
            @SuppressWarnings("null")
            double negativeSampling = os.has("s")  ? (Double)os.valueOf("s") : 5.0;
            execute("crossfold", left, right, gold, nFolds, direction, negativeSampling, outputFile, configuration,
                    os.has("q") ? NONE : STDERR, 
                    new DefaultDatasetLoader());
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

    /** The maximum number of iterations to use in finding a good split */
    public static final int MAX_ITERS = 10;

    private static List<Pair<URIRes, URIRes>> getAllPairs(AlignmentSet alignments) {
        List<Pair<URIRes, URIRes>> pairs = new ArrayList<>();
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
    public static Folds splitDataset(AlignmentSet alignments, int folds, FoldDirection direction) {
        if(folds < 2) { throw new IllegalArgumentException("Cannot do a cross-fold validation on less than 2 folds"); }
        if(direction == null) { throw new IllegalArgumentException("direction is null"); }
        final List<Pair<URIRes, URIRes>> allPairs = getAllPairs(alignments);
        final List<URIRes> leftEntities = new ArrayList<>(allPairs.stream().map(x -> x._1).collect(Collectors.toSet()));
        final List<URIRes> rightEntities = new ArrayList<>(allPairs.stream().map(x -> x._2).collect(Collectors.toSet()));

        Collections.shuffle(leftEntities);
        Collections.shuffle(rightEntities);

        if(direction == FoldDirection.both) {
            final Object2IntMap<URIRes> leftIdx = reverseMap(leftEntities);
            final Object2IntMap<URIRes> rightIdx = reverseMap(rightEntities);

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

            for (Pair<URIRes, URIRes> p : allPairs) {
                forward[leftIdx.getInt(p._1)].add(rightIdx.getInt(p._2));
                backward[rightIdx.getInt(p._2)].add(leftIdx.getInt(p._1));
            }

            //final int[] lsize = new int[folds];
            //final int[] rsize = new int[folds];
            int inlinks = 0;
            final int alllinks = allPairs.size();

            for (int i = 0; i < folds; i++) {
                //lsize[i] = leftFolds[i].size();
                //rsize[i] = rightFolds[i].size();
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
                    mapSplit(rightFolds, rightEntities), direction);
        } else if(direction == FoldDirection.left) {
            List<Set<URIRes>> leftFolds = new ArrayList<>(), rightFolds = new ArrayList<>();
            Set<URIRes> rightElems = new HashSet<>(rightEntities);

            for(int i = 0; i < folds; i++) {
                leftFolds.add(new HashSet<>());
                rightFolds.add(rightElems);
            }

            int n = 0;
            for(URIRes res : leftEntities) {
                leftFolds.get(n++ % folds).add(res);
            }
            return new Folds(leftFolds, rightFolds, direction);
        } else { /* direction == right */
            List<Set<URIRes>> rightFolds = new ArrayList<>(), leftFolds = new ArrayList<>();
            Set<URIRes> leftElems = new HashSet<>(leftEntities);
            for(int i = 0; i < folds; i++) {
                rightFolds.add(new HashSet<>());
                leftFolds.add(leftElems);
            }

            int n = 0;
            for(URIRes res : rightEntities) {
                rightFolds.get(n++ % folds).add(res);
            }
            return new Folds(leftFolds, rightFolds, direction);
        }
    }

    private static class StepResult {

        public final int inlinks;
        public final boolean improved;

        public StepResult(int inlinks, boolean improved) {
            this.inlinks = inlinks;
            this.improved = improved;
        }

    }

    private static StepResult step(final List<URIRes> entities, final IntSet[] links,
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

    private static List<Set<URIRes>> mapSplit(IntSet[] left, List<URIRes> entities) {
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

    private static Object2IntMap<URIRes> reverseMap(List<URIRes> leftEntities) {
        Object2IntMap<URIRes> m = new Object2IntOpenHashMap<>();
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
        public final List<Set<URIRes>> leftSplit, rightSplit;
        private final FoldDirection direction;

        public Folds(List<Set<URIRes>> leftSplit, List<Set<URIRes>> rightSplit, FoldDirection direction) {
            this.leftSplit = leftSplit;
            this.rightSplit = rightSplit;
            this.direction = direction;
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
                if ((!leftSplit.get(foldNo).contains(a.entity1) || direction == FoldDirection.right)
                        && (!rightSplit.get(foldNo).contains(a.entity2) || direction == FoldDirection.left)) {
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < leftSplit.size(); i++) {
                sb.append(leftSplit.get(i).size()).append("/").append(rightSplit.get(i).size()).append(" ");
            }
            return sb.toString();
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
        public void addLensResult(URIRes id1, URIRes id2, String lensId, LensResult res) {
            mem.addLensResult(id1, id2, lensId, res);
        }

        @Override
        public void message(Stage stage, Level level, String message) {
            if (foldNo > 0) {
                mem.message(stage, level, String.format("[Fold %d/%d] %s", foldNo, nFolds, message));
            } else {
                mem.message(stage, level, message);
            }
        }
        
        

    }
}
