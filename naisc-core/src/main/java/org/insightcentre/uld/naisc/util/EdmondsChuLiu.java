package org.insightcentre.uld.naisc.util;

/* Copyright (C) 2009-2011  Syed Asad Rahman <asad@ebi.ac.uk>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

 /*
 * Refer: http://algowiki.net/wiki/index.php/Edmonds's_algorithm
 * Edmonds's - Chu-Liu algorithm finds a minimum/maximum 
 * branching for a directed graph (similar to a minimum spanning tree).
 *
 * <ul>
 *  <li>Remove all edges going into the root node
 *  <li>For each node, select only the incoming edge with smallest weight
 *  <li>For each circuit that is formed:
 *<ul>
 *  <li>edge "m" is the edge in this circuit with minimum weight
 *  <li>Combine all the circuit's nodes into one pseudo-node "k"
 *  <li>For each edge "e" entering a node in "k" in the original graph:
 *<ul>
 *  <li>edge "n" is the edge currently entering this node in the circuit
 *  <li>track the minimum modified edge weight between each "e" based on the following:
 *<ul>
 *  <li>modWeight = weight("e") - ( weight("n") - weight("m") )
 *</ul>
 *</ul>
 *<li>On edge "e" with minimum modified weight, add edge "e" and remove edge "n"
 *</ul>
 * 
 * In high level words
 * 
 * Find a cycle, any cycle.
 * Remove all nodes of the cycle and mark cycles to be broken
 * Recursive: find a cycle, any cycle…
 * If you can’t find a cycle, you hit the terminal case. 
 * Return the current graph (well, the greedy transformation thereof).
 * Now it’s time to break that cycle. Remove the least likely edge.
 * Remove the placeholder node from the graph and put back in all the nodes from the now-broken cycle. 
 * Return.
 * 
 * 
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Derived from https://github.com/asad/GraphMST and cleaned-up significantly
 *
 * @author Asad
 */
public class EdmondsChuLiu {

    //private List<Node> cycle;
    public AdjacencyList getMinBranching(Node root, AdjacencyList list) {
        AdjacencyList reverse = list.getReversedList();
        // remove all edges entering the root
        if (reverse.getAdjacent(root) != null) {
            reverse.getAdjacent(root).clear();
        }
        AdjacencyList outEdges = new AdjacencyList();
        // for each node, select the edge entering it with smallest weight
        for (Node n : reverse.getSourceNodeSet()) {
            List<Edge> inEdges = reverse.getAdjacent(n);
            if (inEdges.isEmpty()) {
                continue;
            }
            Edge min = inEdges.get(0);
            for (Edge e : inEdges) {
                if (e.getWeight() < min.getWeight()) {
                    min = e;
                }
            }
            outEdges.addEdge(min.getTo(), min.getFrom(), min.getWeight());
        }

        // detect cycles
        List<List<Node>> cycles = new ArrayList<>();
        List<Node> cycle = new ArrayList<>();
        getCycle(root, outEdges, cycle);
        cycles.add(cycle);
        for (Node n : outEdges.getSourceNodeSet()) {
            if (!n.isVisited()) {
                cycle = new ArrayList<>();
                getCycle(n, outEdges, cycle);
                cycles.add(cycle);
            }
        }

        // for each cycle formed, modify the path to merge it into another part of the graph
        AdjacencyList outEdgesReverse = outEdges.getReversedList();

        for (List<Node> x : cycles) {
            if (x.contains(root)) {
                continue;
            }
            mergeCycles(x, list, reverse, outEdges, outEdgesReverse);
        }
        return outEdges;
    }

    private void mergeCycles(List<Node> cycle, AdjacencyList list, AdjacencyList reverse, AdjacencyList outEdges, AdjacencyList outEdgesReverse) {
        List<Edge> cycleAllInEdges = new ArrayList<>();
        Edge minInternalEdge = null;
        // find the minimum internal edge weight
        for (Node n : cycle) {
            for (Edge e : reverse.getAdjacent(n)) {
                if (cycle.contains(e.getTo())) {
                    if (minInternalEdge == null || minInternalEdge.getWeight() > e.getWeight()) {
                        minInternalEdge = e;
                        //continue;
                    }
                } else {
                    cycleAllInEdges.add(e);
                }
            }
        }
        assert (minInternalEdge != null);
        // find the incoming edge with minimum modified cost
        Edge minExternalEdge = null;
        double minModifiedWeight = 0;
        for (Edge e : cycleAllInEdges) {
            double w = e.getWeight() - (outEdgesReverse.getAdjacent(e.getFrom()).get(0).getWeight() - minInternalEdge.getWeight());
            if (minExternalEdge == null || minModifiedWeight > w) {
                minExternalEdge = e;
                minModifiedWeight = w;
            }
        }
        assert (minExternalEdge != null);
        // add the incoming edge and remove the inner-circuit incoming edge
        Edge removing = outEdgesReverse.getAdjacent(minExternalEdge.getFrom()).get(0);
        outEdgesReverse.getAdjacent(minExternalEdge.getFrom()).clear();
        outEdgesReverse.addEdge(minExternalEdge.getTo(), minExternalEdge.getFrom(), minExternalEdge.getWeight());
        List<Edge> adj = outEdges.getAdjacent(removing.getTo());
        for (Iterator<Edge> i = adj.iterator(); i.hasNext();) {
            if (i.next().getTo() == removing.getFrom()) {
                i.remove();
                break;
            }
        }
        outEdges.addEdge(minExternalEdge.getTo(), minExternalEdge.getFrom(), minExternalEdge.getWeight());
    }

    private void getCycle(Node n, AdjacencyList outEdges, List<Node> cycle) {
        n.setVisited(true);
        cycle.add(n);
        if (outEdges.getAdjacent(n) == null) {
            return;
        }
        for (Edge e : outEdges.getAdjacent(n)) {
            if (!e.getTo().isVisited()) {
                getCycle(e.getTo(), outEdges, cycle);
            }
        }
    }

    public static class Edge {

        private final Node from;
        private final Node to;
        private final double weight;

        /**
         *
         * @param argFrom
         * @param argTo
         * @param weight
         */
        public Edge(final Node argFrom, final Node argTo, double weight) {
            this.from = argFrom;
            this.to = argTo;
            this.weight = weight;
        }

        /**
         * @return the from
         */
        public Node getFrom() {
            return from;
        }

        /**
         * @return the to
         */
        public Node getTo() {
            return to;
        }

        public double getWeight() {
            return weight;
        }

    }

    public static class Node<Data> {

        private final Data data;
        private boolean visited = false;   // used for Kosaraju's algorithm and Edmonds's algorithm
        //private int lowlink = -1;          // used for Tarjan's algorithm
        //private int index = -1;            // used for Tarjan's algorithm

        /**
         *
         * @param atom
         */
        public Node(final Data atom) {
            this.data = atom;
        }

        public boolean isVisited() {
            return visited;
        }

        public void setVisited(boolean visited) {
            this.visited = visited;
        }

        public Data getData() {
            return data;
        }

    }

    public static class AdjacencyList {

        private final Map<Node, List<Edge>> adjacencies = new HashMap<>();

        public AdjacencyList() {
        }

        public AdjacencyList getReversedList() {
            AdjacencyList newlist = new AdjacencyList();
            for (List<Edge> edges : adjacencies.values()) {
                for (Edge e : edges) {
                    newlist.addEdge(e.getTo(), e.getFrom(), e.getWeight());
                }
            }
            return newlist;
        }

        public List<Edge> getAdjacent(Node source) {
            return adjacencies.get(source);
        }

        public Set<Node> getSourceNodeSet() {
            return adjacencies.keySet();
        }
        
        public Set<Node> getAdjNodes(Node source) {
            if(adjacencies.containsKey(source)) {
                return adjacencies.get(source).stream().map(x -> x.to).collect(Collectors.toSet());
            } else {
                return Collections.EMPTY_SET;
            }
        }

        public void addEdge(Node source, Node target, double weight) {
            List<Edge> list;
            if (!adjacencies.containsKey(source)) {
                list = new ArrayList<>();
                adjacencies.put(source, list);
            } else {
                list = adjacencies.get(source);
            }
            list.add(new Edge(source, target, weight));
        }
    }
}
