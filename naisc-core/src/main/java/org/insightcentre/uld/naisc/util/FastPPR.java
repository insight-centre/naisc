package org.insightcentre.uld.naisc.util;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

/**
 * Implementation of FastPPR algorithm. Derived from
 * https://github.com/plofgren/fast-ppr-scala/blob/master/src/main/scala/soal/fastppr/FastPPR.scala
 * under the Apache license. The original paper is Lofgren, Peter A., et al.
 * "FAST-PPR: scaling personalized pagerank estimation for large graphs." In
 * Proceedings of the 20th ACM SIGKDD international conference on Knowledge
 * discovery and data mining. ACM, 2014.
 *
 * @author John McCrae
 */
public class FastPPR {

    private static class IntMap {

        private static int INIT_CAP = 1024;
        private int[] data;
        private int cap = 0;

        public void put(int i, int j) {
            if (data == null) {
                cap = INIT_CAP;
                while (cap <= i) {
                    cap <<= 1;
                }
                data = new int[cap];
                Arrays.fill(data, -1);
            } else if (i >= cap) {
                while (cap <= i) {
                    cap <<= 1;
                }
                int[] d2 = new int[cap];
                Arrays.fill(d2, -1);
                System.arraycopy(data, 0, d2, 0, data.length);
                data = d2;
            }
            data[i] = j;
        }

        public void remove(int i) {
            data[i] = -1;
        }

        public boolean containsKey(int i) {
            return i < data.length && data[i] != -1;
        }

        public int get(int i) {
            return data[i] == -1 ? 0 : data[i];
        }
    }

    private static class HeapMappedPriorityQueue {

        private final FloatList priorities = new FloatArrayList(); //the first entry will be ignored to make arithmetic simpler

        private final IntMap itemToIndex = new IntMap();
        private final IntList indexToItem;

        public HeapMappedPriorityQueue() {
            indexToItem = new IntArrayList();
            indexToItem.add(0); //the first entry will be ignored to make arithmetic simpler
            priorities.add(0.0f);
        }

        private int parent(int i) {
            return i / 2;
        }

        private int left(int i) {
            return i * 2;
        }

        private int right(int i) {
            return i * 2 + 1;
        }

        private void swap(int i, int j) {
            float temp = priorities.get(i);
            priorities.set(i, priorities.get(j));
            priorities.set(j, temp);

            int itemI = indexToItem.get(i);
            int itemJ = indexToItem.get(j);
            itemToIndex.put(itemI, j);
            itemToIndex.put(itemJ, i);
            indexToItem.set(i, itemJ);
            indexToItem.set(j, itemI);
        }

        /**
         * If the max-heap invariant is satisfied except for index i possibly
         * being smaller than a child, restore the invariant.
         */
        private void maxHeapify(int i) {
            int largest = i;
            if (left(i) < priorities.size() && priorities.get(left(i)) > priorities.get(i)) {
                largest = left(i);
            }
            if (right(i) < priorities.size() && priorities.get(right(i)) > priorities.get(largest)) {
                largest = right(i);
            }
            if (largest != i) {
                swap(i, largest);
                maxHeapify(largest);
            }
        }

        void insert(int a, float priority) {
            itemToIndex.put(a, indexToItem.size());
            indexToItem.add(a);
            priorities.add(Float.NEGATIVE_INFINITY);
            increasePriority(a, priority);
        }

        boolean isEmpty() {
            return indexToItem.size() == 1; // first entry is dummy entry
        }

        int extractMax() {
            if (isEmpty()) {
                throw new NoSuchElementException();
            }
            int maxItem = indexToItem.get(1);
            swap(1, priorities.size() - 1);
            priorities.remove(priorities.size() - 1);
            indexToItem.remove(indexToItem.size() - 1);
            itemToIndex.remove(maxItem);

            maxHeapify(1);
            return maxItem;
        }

        float maxPriority() {
            if (isEmpty()) {
                throw new NoSuchElementException();
            }
            return priorities.get(1);
        }

        float getPriority(int a) {

            if (!itemToIndex.containsKey(a)) {
                return 0.0f;
            } else {
                return priorities.get(itemToIndex.get(a));
            }
        }

        void increasePriority(int a, float newPriority) {
            assert (newPriority >= getPriority(a));
            int i = itemToIndex.get(a);
            priorities.set(i, newPriority);
            while (i > 1 && priorities.get(i) > priorities.get(parent(i))) {
                swap(i, parent(i));
                i = parent(i);
            }
        }

        boolean contains(int a) {
            return itemToIndex.containsKey(a);
        }
    }

    public static class DirectedGraph {

        private final ArrayList<GraphNode> nodes = new ArrayList<>();

        public final SimpleCache<Integer, Pair<Int2FloatMap, Float>> inversePPRBalancedCache = new SimpleCache<>(100);

        public int addNode() {
            int id = nodes.size();
            GraphNode node = new GraphNode(id);
            nodes.add(node);
            return id;
        }

        public void addEdge(int i, int j) {
            nodes.get(i).outboundNodes.add(j);
            nodes.get(j).inboundNodes.add(i);
            inversePPRBalancedCache.clear();
        }

        private GraphNode getNodeById(int startId) {
            return nodes.get(startId);
        }

        @Override
        public String toString() {
            return "DirectedGraph{" + "nodes=" + nodes + '}';
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 13 * hash + Objects.hashCode(this.nodes);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DirectedGraph other = (DirectedGraph) obj;
            if (!Objects.equals(this.nodes, other.nodes)) {
                return false;
            }
            return true;
        }

    }

    private static class GraphNode {

        private final IntSet outboundNodes;
        private final int id;
        private final IntSet inboundNodes;

        private GraphNode(int id) {
            this.id = id;
            this.inboundNodes = new IntOpenHashSet();
            this.outboundNodes = new IntOpenHashSet();
        }

        private int outboundCount() {
            return outboundNodes.size();
        }

        private Option<Integer> randomOutboundNode() {
            if (outboundNodes.isEmpty()) {
                return new None<>();
            } else {
                int i = new Random().nextInt(outboundNodes.size());
                IntIterator iter = outboundNodes.iterator();
                while (i > 0) {
                    i--;
                    iter.next();
                }
                return new Some<>(iter.next());
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + Objects.hashCode(this.outboundNodes);
            hash = 79 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GraphNode other = (GraphNode) obj;
            if (this.id != other.id) {
                return false;
            }
            if (!Objects.equals(this.outboundNodes, other.outboundNodes)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "GraphNode{" + "outboundNodes=" + outboundNodes + ", id=" + id + '}';
        }

    }

    public static class FastPPRConfiguration {

        float pprSignificanceThreshold = 1.0e-3f;
        float reversePPRApproximationFactor = 1.0f / 6.0f;
        float teleportProbability = 0.2f;
        float forwardStepsPerReverseStep = 6.7f;
        float nWalksConstant = (float) (24 * Math.log(1.0e6));
        
        public FastPPRConfiguration() {
        }

        public FastPPRConfiguration(float pprSignificanceThreshold, float reversePPRApproximationFactor, float teleportProbability, float forwardStepsPerReverseStep, float nWalksConstant) {
            this.pprSignificanceThreshold = pprSignificanceThreshold;
            this.reversePPRApproximationFactor = reversePPRApproximationFactor;
            this.teleportProbability = teleportProbability;
            this.forwardStepsPerReverseStep = forwardStepsPerReverseStep;
            this.nWalksConstant = nWalksConstant;
        }

        private int walkCount(float forwardPPRSignificanceThreshold) {
            return (int) (nWalksConstant / forwardPPRSignificanceThreshold);
        }
    }

    /**
     * Returns an estimate of ppr(start, target). Its accuracy depends on the
     * parameters in config. If balanced is true, it attempts to balanced
     * forward and backward work to decrease running time without significantly
     * changing the accuracy.
     *
     * @param graph The graph to traverse
     * @param startId The initial node
     * @param targetId The final node
     * @param config The configuration
     * @return The PPR Score
     */
    public static float estimatePPR(DirectedGraph graph, int startId,
            int targetId, FastPPRConfiguration config) {
        return estimatePPR(graph, startId, targetId, config, true);
    }

    /**
     * Returns an estimate of ppr(start, target). Its accuracy depends on the
     * parameters in config. If balanced is true, it attempts to balanced
     * forward and backward work to decrease running time without significantly
     * changing the accuracy.
     *
     * @param graph The graph to traverse
     * @param startId The initial node
     * @param targetId The final node
     * @param config The configuration
     * @param balanced If balanced is true, it attempts to balanced forward and
     * backward work to decrease running time without significantly changing the
     * accuracy
     * @return The PPR Score
     */
    public static float estimatePPR(DirectedGraph graph, int startId,
            int targetId, FastPPRConfiguration config, boolean balanced) {
        Int2FloatMap inversePPREstimates;
        float reversePPRSignificanceThreshold;
        if (balanced) {
            Pair<Int2FloatMap, Float> r = estimateInversePPRBalanced(graph, targetId, config);
            inversePPREstimates = r._1;
            reversePPRSignificanceThreshold = r._2;
        } else {
            float reverseThreshold = (float) Math.sqrt(config.pprSignificanceThreshold);

            inversePPREstimates = estimateInversePPR(graph, targetId, config, config.reversePPRApproximationFactor * reverseThreshold);
            reversePPRSignificanceThreshold = reverseThreshold;
        }
        IntSet frontier = computeFrontier(graph, inversePPREstimates, reversePPRSignificanceThreshold);
        float forwardPPRSignificanceThreshold = config.pprSignificanceThreshold / reversePPRSignificanceThreshold;

        boolean startNodeInTargetSet = (inversePPREstimates.get(startId) >= reversePPRSignificanceThreshold);
        if (startNodeInTargetSet || frontier.contains(startId)) {
            return inversePPREstimates.get(startId);
        }

        return pprToFrontier(graph, startId, forwardPPRSignificanceThreshold, config, frontier, inversePPREstimates);
    }

    private static final Random random = new Random();

    /**
     * Returns an estimate of the PPR from start to the frontier, using weights
     * in inversePPREstimates.
     */
    private static float pprToFrontier(DirectedGraph graph, int startId,
            float forwardPPRSignificanceThreshold, FastPPRConfiguration config,
            IntSet frontier, Int2FloatMap inversePPREstimates) {
        int walkCount = config.walkCount(forwardPPRSignificanceThreshold);
        float estimate = 0.0f;
        for (int walkIndex = 0; walkIndex < walkCount; walkIndex++) {
            GraphNode currentNode = graph.getNodeById(startId);
            while (random.nextFloat() > config.teleportProbability
                    && currentNode.outboundCount() > 0
                    && !frontier.contains(currentNode.id)) {
                currentNode = graph.getNodeById(currentNode.randomOutboundNode().get());
            }
            if (frontier.contains(currentNode.id)) {
                estimate += 1.0 / walkCount * inversePPREstimates.get(currentNode.id);
            }

        }
        return estimate;
    }

    private static Int2FloatMap monteCarloPPR(DirectedGraph graph, int startId,
            int walkCount, float teleportProbability) {

        Int2FloatMap pprEstimates = new Int2FloatOpenHashMap();
        for (int walkIndex = 0; walkIndex < walkCount; walkIndex++) {
            GraphNode currentNode = graph.getNodeById(startId);
            boolean hitDeadEnd = false;
            while (random.nextFloat() > teleportProbability && !hitDeadEnd) {
                if (currentNode.outboundCount() > 0) {
                    currentNode = graph.getNodeById(currentNode.randomOutboundNode().get());
                } else {
                    hitDeadEnd = true;
                }
            }
            if (!hitDeadEnd) {
                pprEstimates.put(currentNode.id, pprEstimates.get(currentNode.id) + 1.0f / walkCount);
            }
        }
        return pprEstimates;
    }

    /**
     * Returns a map from nodeId to ppr(node, target) up to a fixed additive
     * accuracy pprErrorTolerance.
     */
    private static Int2FloatMap estimateInversePPR(DirectedGraph graph, int targetId,
            FastPPRConfiguration config, float pprErrorTolerance) {
        // Use ArrayDequeue because it is more efficient than the linked-list based mutable.Queue
        ArrayDeque<Integer> largeResidualNodes = new ArrayDeque<>();
        largeResidualNodes.add(targetId);

        // inversePPREstimates(uId) estimates ppr(u, target)
        Int2FloatMap inversePPREstimates = new Int2FloatOpenHashMap();
        Int2FloatMap inversePPRResiduals = new Int2FloatOpenHashMap();
        inversePPREstimates.put(targetId, config.teleportProbability);
        inversePPRResiduals.put(targetId, config.teleportProbability);

        float largeResidualThreshold = pprErrorTolerance * config.teleportProbability; // inversePPRResiduals about this must be enqueued and pushed

        while (!largeResidualNodes.isEmpty()) {
            int vId = largeResidualNodes.pollFirst();
            float vResidual = inversePPRResiduals.get(vId);
            inversePPRResiduals.put(vId, 0.0f);
            GraphNode v = graph.getNodeById(vId);
            for (int uId : v.inboundNodes) {
                GraphNode u = graph.getNodeById(uId);
                float deltaPriority = (1.0f - config.teleportProbability) / u.outboundCount() * vResidual;
                inversePPRResiduals.put(uId, inversePPRResiduals.get(uId) + deltaPriority);
                inversePPREstimates.put(uId, inversePPREstimates.get(uId) + deltaPriority);
                if (inversePPRResiduals.get(uId) >= largeResidualThreshold && inversePPRResiduals.get(uId) - deltaPriority < largeResidualThreshold) {
                    largeResidualNodes.add(uId);
                }

            }
        }

        debias(graph, config, inversePPREstimates, pprErrorTolerance / config.reversePPRApproximationFactor, pprErrorTolerance);

        return inversePPREstimates;
    }

    private static long predictedForwardSteps(float largestResidual, FastPPRConfiguration config) {
        float reverseThreshold = largestResidual / config.teleportProbability / config.reversePPRApproximationFactor;
        if (reverseThreshold < config.pprSignificanceThreshold) {
            return 0; // avoid division by 0 if reversePPRThreshold==0.0f
        } else {
            float forwardThreshold = config.pprSignificanceThreshold / reverseThreshold;
            return (long) (config.walkCount(forwardThreshold) / config.teleportProbability);
        }
    }

    /**
     * Computes inversePPR to the target up to a dynamic accuracy with the goal
     * of balancing forward and reverse work.
     *
     * @return (inversePPREstimates, reversePPRSignificanceThreshold)
     */
    private static Pair<Int2FloatMap, Float> estimateInversePPRBalanced(DirectedGraph graph, int targetId, FastPPRConfiguration config) {
        return graph.inversePPRBalancedCache.get(targetId, id -> _estimateInversePPRBalanced(graph, id, config));
    }

    private static Pair<Int2FloatMap, Float> _estimateInversePPRBalanced(DirectedGraph graph, int targetId, FastPPRConfiguration config) {
        HeapMappedPriorityQueue inversePPRResiduals = new HeapMappedPriorityQueue();
        Int2FloatMap inversePPREstimates = new Int2FloatOpenHashMap(); // inversePPREstimates(uId) estimates ppr(u, target)
        inversePPRResiduals.insert(targetId, config.teleportProbability);
        inversePPREstimates.put(targetId, config.teleportProbability);

        long reverseSteps = 0L;

        while (!inversePPRResiduals.isEmpty()
                && predictedForwardSteps(inversePPRResiduals.maxPriority(), config) * config.forwardStepsPerReverseStep >= reverseSteps) {
            float vPriority = inversePPRResiduals.maxPriority();
            int vId = inversePPRResiduals.extractMax();
            GraphNode v = graph.getNodeById(vId);
            for (int uId : v.inboundNodes) {
                GraphNode u = graph.getNodeById(uId);
                float deltaPriority = (1.0f - config.teleportProbability) / u.outboundCount() * vPriority;
                if (!inversePPRResiduals.contains(uId)) {
                    inversePPRResiduals.insert(uId, 0.0f);
                }
                inversePPRResiduals.increasePriority(uId, inversePPRResiduals.getPriority(uId) + deltaPriority);
                inversePPREstimates.put(uId, inversePPREstimates.get(uId) + deltaPriority);
                reverseSteps += 1;
            }
        }
        final float pprErrorTolerance;

        if (inversePPRResiduals.isEmpty()) {
            pprErrorTolerance = 0.0f;
        } else {
            pprErrorTolerance = inversePPRResiduals.maxPriority() / config.teleportProbability;
        }
        float reversePPRSignificanceThreshold = pprErrorTolerance / config.reversePPRApproximationFactor;

        debias(graph, config, inversePPREstimates, reversePPRSignificanceThreshold, pprErrorTolerance);

        return new Pair<>(inversePPREstimates, reversePPRSignificanceThreshold);
    }

    /**
     * Modifies inversePPREstimates to remove the negative bias. Given estimates
     * are within an interval estimate <= trueValue <= estimate +
     * pprErrorTolerance This function heuristically centers the estimates in
     * the target set, and propagates those new estimates to the frontier.
     */
    private static void debias(DirectedGraph graph, FastPPRConfiguration config, Int2FloatMap inversePPREstimates, float reversePPRSignificanceThreshold, float pprErrorTolerance) {
        for (int vId : inversePPREstimates.keySet()) {
            if (inversePPREstimates.get(vId) > reversePPRSignificanceThreshold) {
                inversePPREstimates.put(vId, inversePPREstimates.get(vId) + pprErrorTolerance / 2.0f);
                GraphNode v = graph.getNodeById(vId);
                for (int uId : v.inboundNodes) {
                    GraphNode u = graph.getNodeById(uId);
                    inversePPREstimates.put(uId, inversePPREstimates.get(uId) + (1.0f - config.teleportProbability) / u.outboundCount() * pprErrorTolerance / 2.0f);
                }
            }
        }
    }

    /**
     * Returns the set of nodes with some out-neighbor in the target set (those
     * nodes v with ppr(v, target) > reversePPRSignificanceThreshold)
     */
    private static IntSet computeFrontier(DirectedGraph graph, Int2FloatMap inversePPREstimates, float reversePPRSignificanceThreshold) {
        IntSet frontier = new IntOpenHashSet();
        for (int vId : inversePPREstimates.keySet()) {
            boolean vInTargetSet = (inversePPREstimates.get(vId) >= reversePPRSignificanceThreshold);
            if (vInTargetSet) {
                GraphNode v = graph.getNodeById(vId);
                for (int uId : v.inboundNodes) {
                    frontier.add(uId);
                }
            }
        }
        return frontier;
    }
}
