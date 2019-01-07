package org.insightcentre.uld.naisc.main;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Perform a cross-fold evaluation of a configuration on a particular dataset.
 *
 * @author John McCrae
 */
public class CrossFold {

    private List<Pair<Resource, Resource>> getAllPairs(Map<Property, Object2DoubleMap<Statement>> alignments) {
        List<Pair<Resource, Resource>> pairs = new ArrayList<>();
        for (Map.Entry<Property, Object2DoubleMap<Statement>> e : alignments.entrySet()) {
            for (Statement s : e.getValue().keySet()) {
                if (s.getObject().isURIResource()) {
                    pairs.add(new Pair(s.getSubject(), s.getObject()));
                }
            }
        }
        return pairs;
    }

    /**
     * Split the dataset, randomly but heuristically. This method attempts to
     * group the entities according to two measures, firstly <b>balance</b>,
     * which is based on the Fano factor of the dist. Secondly,
     * <b>precision</b> which is the number of links in the same fold, divided
     * by the total number of links. This method greedily attempts to minimize
     * the harmonic mean of this.
     *
     * @param alignments The alignment data
     * @param folds The number of folds
     * @return Returns the split of entities on the left (subject) side and
     * right (object) side
     */
    public Result splitDataset(Map<Property, Object2DoubleMap<Statement>> alignments, int folds) {
        final List<Pair<Resource, Resource>> allPairs = getAllPairs(alignments);
        final List<Resource> leftEntities = allPairs.stream().map(x -> x._1).collect(Collectors.toList());
        final List<Resource> rightEntities = allPairs.stream().map(x -> x._2).collect(Collectors.toList());

        Collections.shuffle(leftEntities);
        Collections.shuffle(rightEntities);

        final Object2IntMap<Resource> leftIdx = reverseMap(leftEntities);
        final Object2IntMap<Resource> rightIdx = reverseMap(rightEntities);

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

        for (Pair<Resource, Resource> p : allPairs) {
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

        double lbalance = balance(lsize);
        double rbalance = balance(rsize);
        double precision = (double) inlinks / (double) alllinks;
        double lscore = 2.0 * lbalance * precision / (lbalance + precision);
        double rscore = 2.0 * rbalance * precision / (rbalance + precision);

        boolean improved = true;

        while (improved) {
            improved = false;
            StepResult res = step(leftEntities, folds, forward, rightFolds, byLeftFold, inlinks, alllinks, lbalance, lscore, lsize, leftFolds);
            if (res.improved) {
                improved = true;
                inlinks = res.inlinks;
                lbalance = res.balance;
                precision = (double) inlinks / (double) alllinks;
                lscore = 2.0 * lbalance * precision / (lbalance + precision);
                rscore = 2.0 * rbalance * precision / (rbalance + precision);
            }
            res = step(rightEntities, folds, backward, leftFolds, byRightFold, inlinks, alllinks, rbalance, rscore, rsize, rightFolds);
            if (res.improved) {
                improved = true;
                inlinks = res.inlinks;
                rbalance = res.balance;
                precision = (double) inlinks / (double) alllinks;
                lscore = 2.0 * lbalance * precision / (lbalance + precision);
                rscore = 2.0 * rbalance * precision / (rbalance + precision);
            }
        }

        return new Result(mapSplit(leftFolds, leftEntities),
                mapSplit(rightFolds, rightEntities));
    }

    private static class StepResult {

        public final int inlinks;
        public final double balance;
        public final boolean improved;

        public StepResult(int inlinks, double balance, boolean improved) {
            this.inlinks = inlinks;
            this.balance = balance;
            this.improved = improved;
        }

    }

    private StepResult step(final List<Resource> entities, final int folds, final IntSet[] links,
            final IntSet[] reverseFolds, final int[] byFold,
            int inlinks, final int alllinks, double balance, double _score,
            final int[] lsize, final IntSet[] leftFolds) {
        boolean improved = false;
        double precision;
        for (int l = 0; l < entities.size(); l++) {
            double[] score = new double[entities.size() + folds];
            int[] dinlinks = new int[entities.size() + folds];
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
                score[l2] = 2.0 * balance * precision2 / (balance + precision2) - _score;
            }
            for (int i = 0; i < folds; i++) {
                for (int t : links[l]) {
                    if (reverseFolds[i].contains(t)) {
                        dinlinks[entities.size() + i] += 1;
                    }
                }
                double precision2 = (double) (inlinks + dinlinks[entities.size() + i]) / alllinks;
                lsize[byFold[l]]--;
                lsize[i]++;
                double lbalance2 = balance(lsize);
                lsize[byFold[l]]++;
                lsize[i]--;
                score[entities.size() + i] = 2.0 * lbalance2 * precision2 / (lbalance2 + precision2) - _score;
            }
            int best = whichMax(score);
            if (score[best] > 0) {
                if (best < entities.size()) {
                    int fold1 = byFold[l];
                    int fold2 = byFold[best];
                    if (fold1 == fold2) {
                        throw new RuntimeException();
                    }
                    System.err.println(String.format("Swapping %d with %d", l, best));
                    inlinks += dinlinks[best];
                    precision = (double) inlinks / alllinks;
                    _score = 2.0 * balance * precision / (balance + precision);
                    leftFolds[fold1].remove(l);
                    leftFolds[fold1].add(best);
                    leftFolds[fold2].add(l);
                    leftFolds[fold2].remove(best);
                    byFold[l] = fold2;
                    byFold[best] = fold1;
                } else {
                    int fold1 = byFold[l];
                    int fold2 = best - entities.size();
                    System.err.println(String.format("Moving %d to %d", l, fold2));
                    inlinks += dinlinks[best];
                    precision = (double) inlinks / alllinks;
                    lsize[fold1]--;
                    lsize[fold2]++;
                    balance = balance(lsize);
                    _score = 2.0 * balance * precision / (balance + precision);
                    leftFolds[fold1].remove(l);
                    leftFolds[fold2].add(l);
                    byFold[l] = fold2;
                }
                improved = true;
            }
        }
        return new StepResult(inlinks, balance, improved);

    }

    private List<Set<Resource>> mapSplit(IntSet[] left, List<Resource> entities) {
        return Arrays.asList(left).stream().map(x -> x.stream().map(y -> entities.get(y)).collect(Collectors.toSet())).collect(Collectors.toList());
    }

    private int whichMax(double[] score) {
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

    /**
     * The Fano Factor is defined F = var(x) / mean(x). This metric is changed
     * to return 1 - F / sum(x) as this is bounded between 0 and 1 and the
     * maximum is given when the distribution is well-balanced. We also apply
     * add-one alpha smoothing
     */
    private double balance(int[] sizes) {
        int sumxx = 0;
        int sumx = 0;
        int N = sizes.length;
        for (int s : sizes) {
            sumxx += (s + 1) * (s + 1);
            sumx += s + 1;
        }
        double F = (double) (N * sumxx - sumx * sumx) / (double) ((N - 1) * sumx);
        return 1.0 - F / sumx;
    }

    private Object2IntMap<Resource> reverseMap(List<Resource> leftEntities) {
        Object2IntMap<Resource> m = new Object2IntOpenHashMap<>();
        for (int i = 0; i < leftEntities.size(); i++) {
            m.put(leftEntities.get(i), i);
        }
        return m;
    }

    /**
     * The result of a cross-fold split
     */
    public static class Result {

        /**
         * The left and right split
         */
        public final List<Set<Resource>> leftSplit, rightSplit;

        public Result(List<Set<Resource>> leftSplit, List<Set<Resource>> rightSplit) {
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
        public Map<Property, Object2DoubleMap<Statement>> train(Map<Property, Object2DoubleMap<Statement>> alignments, int foldNo) {
            Map<Property, Object2DoubleMap<Statement>> m = new HashMap<>();
            for (Map.Entry<Property, Object2DoubleMap<Statement>> e : alignments.entrySet()) {
                Object2DoubleMap<Statement> m2 = new Object2DoubleOpenHashMap<>();
                m.put(e.getKey(), m2);
                for (Object2DoubleMap.Entry<Statement> e2 : e.getValue().object2DoubleEntrySet()) {
                    if (!leftSplit.get(foldNo).contains(e2.getKey().getSubject())
                            && !rightSplit.get(foldNo).contains(e2.getKey().getObject())) {
                        m2.put(e2.getKey(), e2.getDoubleValue());
                    }
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
        public Map<Property, Object2DoubleMap<Statement>> test(Map<Property, Object2DoubleMap<Statement>> alignments, int foldNo) {
            Map<Property, Object2DoubleMap<Statement>> m = new HashMap<>();
            for (Map.Entry<Property, Object2DoubleMap<Statement>> e : alignments.entrySet()) {
                Object2DoubleMap<Statement> m2 = new Object2DoubleOpenHashMap<>();
                m.put(e.getKey(), m2);
                for (Object2DoubleMap.Entry<Statement> e2 : e.getValue().object2DoubleEntrySet()) {
                    if (leftSplit.get(foldNo).contains(e2.getKey().getSubject())
                            && rightSplit.get(foldNo).contains(e2.getKey().getObject())) {
                        m2.put(e2.getKey(), e2.getDoubleValue());
                    }
                }
            }
            return m;
        }
    }
}
