package org.insightcentre.uld.naisc.matcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.BitSet;
import java.util.Map;
import java.util.Random;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.MatcherFactory;
import org.insightcentre.uld.naisc.constraint.Constraint;
import org.insightcentre.uld.naisc.constraint.UnsolvableConstraint;
import org.insightcentre.uld.naisc.main.ExecuteListener;

/**
 *
 * @author John McCrae
 */
public class MonteCarloTreeSearch implements MatcherFactory {

    @Override
    public String id() {
        return "monte-carlo";
    }

    @Override
    public Matcher makeMatcher(Map<String, Object> params) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        return new MonteCarloTreeSearchImpl(config.constraint.make(), config.maxIterations, config.ce);
    }

    public static class Configuration {

        /**
         * The exploration parameter (expert)
         */
        @ConfigurationParameter(description = "The exploration paramter (expert)")
        public double ce = 2.0;

        /**
         * The maximum number of iterations to perform
         */
        @ConfigurationParameter(description = "The maxiumum number of iterations to perform")
        public int maxIterations = 100000;

        /**
         * The constraint that the searcher will optimize
         */
        @ConfigurationParameter(description = "The constraint that the searcher will optimize")
        public org.insightcentre.uld.naisc.main.Configuration.ConstraintConfiguration constraint;
    }

    private static class MonteCarloTreeSearchImpl implements Matcher {

        private final Constraint initial;
        private final int iterMax;
        private final double ce;
        private final Random random = new Random();

        public MonteCarloTreeSearchImpl(Constraint initial, int iterMax, double ce) {
            this.initial = initial;
            this.iterMax = iterMax;
            this.ce = ce;
        }

        @Override
        public AlignmentSet alignWith(AlignmentSet matches, AlignmentSet partialAlign, ExecuteListener listener) {
            matches.sortAlignments();
            Constraint base = initial.copy();
            for (Alignment init : partialAlign) {
                if (!base.canAdd(init)) {
                    listener.updateStatus(ExecuteListener.Stage.MATCHING, "A link from the initial set is not valid with the constraint.");
                }
                base.add(init);
                matches.remove(init);
            }
            Constraint bestValid = base.complete() ? base : null;

            MCTSTree root = new MCTSTree(randomSearch(base.copy(), matches, 0));
            ITERATION:
            for (int iter = 0; iter < iterMax; iter++) {
                if ((iter + 1) % 10000 == 0 && listener != null) {
                    listener.updateStatus(ExecuteListener.Stage.MATCHING,
                            String.format("Generated %sth candidate (max probability=%.2f)", iter,
                                    bestValid == null ? 0.0 : bestValid.score));
                }

                Constraint soln = base.copy();
                MCTSTree tree = root;
                BitSet replay = new BitSet(matches.size());
                int replaySize = 0;
                int depth = 0;
                while (depth < matches.size()) {
                    if (!soln.canAdd(matches.get(depth))) {
                        if (tree.right != null) {
                            replay.set(depth, false);
                            replaySize++;
                            tree = tree.right;
                            depth++;
                        } else {
                            double score = randomSearch(soln, matches, depth + 1);
                            tree.right = new MCTSTree(score);
                            tree.hasLeft = false;
                            backtrack(root, replay, replaySize, score);
                            if (soln.complete() && (bestValid == null || bestValid.score < soln.score)) {
                                bestValid = soln.copy();
                            }
                            break;
                        }
                    } else if (tree.goLeft(ce, matches.size() - depth)) {
                        if (tree.left != null) {
                            replay.set(depth, true);
                            replaySize++;
                            tree = tree.left;
                            soln.add(matches.get(depth));
                            depth++;
                        } else {
                            soln.add(matches.get(depth));
                            double score = randomSearch(soln, matches, depth + 1);
                            tree.left = new MCTSTree(score);
                            backtrack(root, replay, replaySize, score);
                            if (soln.complete() && (bestValid == null || bestValid.score < soln.score)) {
                                bestValid = soln.copy();
                            }
                            break;
                        }
                    } else {
                        if (tree.right != null) {
                            replay.set(depth, false);
                            replaySize++;
                            tree = tree.right;
                            depth++;
                        } else {
                            double score = randomSearch(soln, matches, depth + 1);
                            tree.right = new MCTSTree(score);
                            backtrack(root, replay, replaySize, score);
                            if (soln.complete() && (bestValid == null || bestValid.score < soln.score)) {
                                bestValid = soln.copy();
                            }
                            break;
                        }
                    }
                }
                if (depth == matches.size()) {
                    backtrack(root, replay, replaySize, soln.score);
                    if (soln.complete() && (bestValid == null || bestValid.score < soln.score)) {
                        bestValid = soln.copy();
                    }

                }
                if (root.fullyExpanded(matches.size())) {
                    break;
                }
            }

            if (bestValid != null) {
                return new AlignmentSet(bestValid.alignments());
            } else {
                throw new UnsolvableConstraint("Monte Carlo search did not find any complete solutions");
            }
        }

        private double randomSearch(Constraint score, AlignmentSet matches, int depth) {

            for (int i = depth; i < matches.size(); i++) {
                if (random.nextBoolean()) {
                    if (score.canAdd(matches.get(i))) {
                        score.add(matches.get(i));
                    }
                }
            }
            return score.score;
        }

        private void backtrack(MCTSTree tree, BitSet replay, int replaySize, double score) {
            tree.update(score);
            for (int i = 0; i < replaySize; i++) {
                assert (tree != null);
                if (replay.get(i)) {
                    tree = tree.left;
                } else {
                    tree = tree.right;
                }

                if (tree != null) {
                    tree.update(score);
                }

            }
        }

    }

    private static class MCTSTree {

        public MCTSTree left, right;
        public double sum, sumSq, lb, ub;
        public int visits;
        public boolean hasLeft = true;

        public MCTSTree(double y) {
            sum = y;
            sumSq = y * y;
            lb = y;
            ub = y;
            visits = 1;
        }

        public boolean goLeft(double ce, int depthToGo) {
            if (!hasLeft) {
                return false;
            }
            if (left == null) {
                return true;
            }
            if (right == null) {
                return false;
            }

            if (left.fullyExpanded(depthToGo - 1)) {
                return false;
            }
            if (right.fullyExpanded(depthToGo - 1)) {
                return true;
            }

            double leftScore = lmean() + Math.sqrt(ce * Math.log(visits) / left.visits
                    * Math.min(0.25, lvar()
                            + Math.sqrt(2 * Math.log(visits) / left.visits)));
            double rightScore = rmean() + Math.sqrt(ce * Math.log(visits) / right.visits
                    * Math.min(0.25, rvar()
                            + Math.sqrt(2 * Math.log(visits) / right.visits)));
            return leftScore > rightScore;
        }

        public double lmean() {
            return left.sum / (ub - lb) / left.visits - lb / (ub - lb);
        }

        public double rmean() {
            return right.sum / (ub - lb) / right.visits - lb / (ub - lb);
        }

        public double lvar() {
            if (left.visits <= 1) {
                return Double.POSITIVE_INFINITY;
            }
            return ((left.sumSq - 2.0 * lb * left.sum + left.visits * lb * lb) / (ub - lb) / (ub - lb)
                    - (left.sum / (ub - lb) - left.visits * lb / (ub - lb))
                    * (left.sum / (ub - lb) - left.visits * lb / (ub - lb)) / left.visits) / (left.visits - 1);
        }

        public double rvar() {
            if (right.visits <= 1) {
                return Double.POSITIVE_INFINITY;
            }
            return ((right.sumSq - 2.0 * lb * right.sum + right.visits * lb * lb) / (ub - lb) / (ub - lb)
                    - (right.sum / (ub - lb) - right.visits * lb / (ub - lb))
                    * (right.sum / (ub - lb) - right.visits * lb / (ub - lb)) / right.visits) / (right.visits - 1);
        }

        public void update(double y) {
            sum += y;
            sumSq += y * y;
            visits++;
            ub = Math.max(ub, y);
            lb = Math.min(lb, y);
        }

        private boolean isFullyExpanded = false;

        public boolean fullyExpanded(int depth) {
            return isFullyExpanded = isFullyExpanded
                    || depth <= 0 || (left != null && right != null && left.fullyExpanded(depth - 1) && right.fullyExpanded(depth - 1))
                    || (!hasLeft && right != null && right.fullyExpanded(depth - 1));
        }

    }
}
