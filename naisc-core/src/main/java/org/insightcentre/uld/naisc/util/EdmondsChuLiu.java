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
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Derived from https://github.com/asad/GraphMST and cleaned-up significantly
 *
 * @author Asad
 */
public class EdmondsChuLiu {

    //private List<Node> cycle;
    public static <Data,EdgeType> AdjacencyList<Data,EdgeType> getMinBranching(Node<Data> root, AdjacencyList<Data,EdgeType> list) {
        AdjacencyList<Data,EdgeType> reverse = list.getReversedList();
        // remove all edges entering the root
        if (reverse.getAdjacent(root) != null) {
            reverse.getAdjacent(root).clear();
        }
        AdjacencyList<Data,EdgeType> outEdges = new AdjacencyList();
        // for each node, select the edge entering it with smallest weight
        for (Node<Data> n : reverse.getSourceNodeSet()) {
            List<Edge<Data, EdgeType>> inEdges = reverse.getAdjacent(n);
            if (inEdges.isEmpty()) {
                continue;
            }
            Edge<Data, EdgeType> min = inEdges.get(0);
            for (Edge<Data, EdgeType> e : inEdges) {
                if (e.getWeight() < min.getWeight()) {
                    min = e;
                }
            }
            outEdges.addEdge(min.getTo(), min.getFrom(), min.getWeight(), min.getEdgeType());
        }

        // detect cycles
        List<List<Node<Data>>> cycles = new ArrayList<>();
        List<Node<Data>> cycle = new ArrayList<>();
        getCycle(root, outEdges, cycle);
        cycles.add(cycle);
        for (Node<Data> n : outEdges.getSourceNodeSet()) {
            if (!n.isVisited()) {
                cycle = new ArrayList<>();
                getCycle(n, outEdges, cycle);
                cycles.add(cycle);
            }
        }

        // for each cycle formed, modify the path to merge it into another part of the graph
        AdjacencyList outEdgesReverse = outEdges.getReversedList();

        for (List<Node<Data>> x : cycles) {
            if (x.contains(root)) {
                continue;
            }
            mergeCycles(x, list, reverse, outEdges, outEdgesReverse);
        }
        return outEdges;
    }

    private static <Data,EdgeType> void mergeCycles(List<Node<Data>> cycle, AdjacencyList<Data,EdgeType> list, AdjacencyList<Data,EdgeType> reverse, AdjacencyList<Data,EdgeType> outEdges, AdjacencyList<Data,EdgeType> outEdgesReverse) {
        List<Edge<Data, EdgeType>> cycleAllInEdges = new ArrayList<>();
        Edge<Data, EdgeType> minInternalEdge = null;
        // find the minimum internal edge weight
        for (Node n : cycle) {
            for (Edge<Data, EdgeType> e : reverse.getAdjacent(n)) {
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
        Edge<Data, EdgeType> minExternalEdge = null;
        double minModifiedWeight = 0;
        for (Edge<Data, EdgeType> e : cycleAllInEdges) {
            double w = e.getWeight() - (outEdgesReverse.getAdjacent(e.getFrom()).get(0).getWeight() - minInternalEdge.getWeight());
            if (minExternalEdge == null || minModifiedWeight > w) {
                minExternalEdge = e;
                minModifiedWeight = w;
            }
        }
        assert (minExternalEdge != null);
        // add the incoming edge and remove the inner-circuit incoming edge
        Edge<Data, EdgeType> removing = outEdgesReverse.getAdjacent(minExternalEdge.getFrom()).get(0);
        outEdgesReverse.getAdjacent(minExternalEdge.getFrom()).clear();
        outEdgesReverse.addEdge(minExternalEdge.getTo(), minExternalEdge.getFrom(), minExternalEdge.getWeight(), minExternalEdge.getEdgeType());
        List<Edge<Data, EdgeType>> adj = outEdges.getAdjacent(removing.getTo());
        for (Iterator<Edge<Data, EdgeType>> i = adj.iterator(); i.hasNext();) {
            if (i.next().getTo() == removing.getFrom()) {
                i.remove();
                break;
            }
        }
        outEdges.addEdge(minExternalEdge.getTo(), minExternalEdge.getFrom(), minExternalEdge.getWeight(), minExternalEdge.getEdgeType());
    }

    private static <Data,EdgeType> void getCycle(Node<Data> n, AdjacencyList<Data,EdgeType> outEdges, List<Node<Data>> cycle) {
        n.setVisited(true);
        cycle.add(n);
        if (outEdges.getAdjacent(n) == null) {
            return;
        }
        for (Edge<Data, EdgeType> e : outEdges.getAdjacent(n)) {
            if (!e.getTo().isVisited()) {
                getCycle(e.getTo(), outEdges, cycle);
            }
        }
    }

    public static class Edge<Data, EType> {

        private final Node<Data> from;
        private final Node<Data> to;
        private final double weight;
        private final EType edgeType;

        /**
         *
         * @param argFrom The source node
         * @param argTo The target node
         * @param weight A weighting score
         * @param edgeType A type of the edge (may be null)
         */
        public Edge(final Node argFrom, final Node argTo, double weight, EType edgeType) {
            this.from = argFrom;
            this.to = argTo;
            this.weight = weight;
            this.edgeType = edgeType;
        }

        /**
         * @return the from
         */
        public Node<Data> getFrom() {
            return from;
        }

        /**
         * @return the to
         */
        public Node<Data> getTo() {
            return to;
        }

        public double getWeight() {
            return weight;
        }

        public EType getEdgeType() {
            return edgeType;
        }

        
    }

    public static class Node<Data> {

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.data);
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
            final Node<?> other = (Node<?>) obj;
            if (!Objects.equals(this.data, other.data)) {
                return false;
            }
            return true;
        }

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

    public static class AdjacencyList<Data, EdgeType> {

        private final Map<Node<Data>, List<Edge<Data, EdgeType>>> adjacencies = new HashMap<>();

        public AdjacencyList() {
        }

        public AdjacencyList getReversedList() {
            AdjacencyList newlist = new AdjacencyList();
            for (List<Edge<Data, EdgeType>> edges : adjacencies.values()) {
                for (Edge<Data, EdgeType> e : edges) {
                    newlist.addEdge(e.getTo(), e.getFrom(), e.getWeight(), e.getEdgeType());
                }
            }
            return newlist;
        }

        public List<Edge<Data, EdgeType>> getAdjacent(Node source) {
            return adjacencies.get(source);
        }

        public Set<Node<Data>> getSourceNodeSet() {
            return adjacencies.keySet();
        }
        
        public Set<Node> getAdjNodes(Node source) {
            if(adjacencies.containsKey(source)) {
                return adjacencies.get(source).stream().map(x -> x.to).collect(Collectors.toSet());
            } else {
                return Collections.EMPTY_SET;
            }
        }

        public void addEdge(Node source, Node target, double weight, EdgeType edgeType) {
            List<Edge<Data, EdgeType>> list;
            if (!adjacencies.containsKey(source)) {
                list = new ArrayList<>();
                adjacencies.put(source, list);
            } else {
                list = adjacencies.get(source);
            }
            if(!adjacencies.containsKey(target)) {
                adjacencies.put(target, new ArrayList<>());
            }
            list.add(new Edge<Data, EdgeType>(source, target, weight, edgeType));
        }
        
        public void addAll(AdjacencyList<Data, EdgeType> g) {
            this.adjacencies.putAll(g.adjacencies);
        }
        
        public Set<Node<Data>> getRoots() {
            Set<Node<Data>> roots = new HashSet<>(adjacencies.keySet());
            for(List<Edge<Data, EdgeType>> es : adjacencies.values()) {
                for(Edge<Data, EdgeType> e : es) {
                    roots.remove(e.to);
                }
            }
            return roots;
        }
        
        public boolean accessibleFrom(Node<Data> data) {
            Set<Node<Data>> accessible = new HashSet<>();
            accessible.add(data);
            calcAccessible(accessible, data);
            return accessible.size() == adjacencies.size();
        }
        
        private void calcAccessible(Set<Node<Data>> accessible, Node<Data> d) {
            for(Edge<Data, EdgeType> e : adjacencies.get(d)) {
                if(!accessible.contains(e.to)) {
                    accessible.add(e.to);
                    calcAccessible(accessible, d);
                }
            }
        }
    }
}
