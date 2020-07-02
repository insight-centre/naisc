package org.insightcentre.uld.naisc.meas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;

/**
 *
 * @author John McCrae
 */
public class DataView {

    public static final String[] BLACK_PROPERTIES = new String[]{
        "http://www.w3.org/2002/07/owl#allValuesFrom",
        "http://www.w3.org/2002/07/owl#annotatedProperty",
        "http://www.w3.org/2002/07/owl#annotatedSource",
        "http://www.w3.org/2002/07/owl#annotatedTarget",
        "http://www.w3.org/2002/07/owl#assertionProperty",
        "http://www.w3.org/2002/07/owl#cardinality",
        "http://www.w3.org/2002/07/owl#complementOf",
        "http://www.w3.org/2002/07/owl#datatypeComplementOf",
        "http://www.w3.org/2002/07/owl#differentFrom",
        "http://www.w3.org/2002/07/owl#disjointUnionOf",
        "http://www.w3.org/2002/07/owl#disjointWith",
        "http://www.w3.org/2002/07/owl#distinctMembers",
        "http://www.w3.org/2002/07/owl#equivalentClass",
        "http://www.w3.org/2002/07/owl#equivalentProperty",
        "http://www.w3.org/2002/07/owl#hasKey",
        "http://www.w3.org/2002/07/owl#hasSelf",
        "http://www.w3.org/2002/07/owl#hasValue",
        "http://www.w3.org/2002/07/owl#intersectionOf",
        "http://www.w3.org/2002/07/owl#inverseOf",
        "http://www.w3.org/2002/07/owl#maxCardinality",
        "http://www.w3.org/2002/07/owl#maxQualifiedCardinality",
        "http://www.w3.org/2002/07/owl#members",
        "http://www.w3.org/2002/07/owl#minCardinality",
        "http://www.w3.org/2002/07/owl#minQualifiedCardinality",
        "http://www.w3.org/2002/07/owl#onClass",
        "http://www.w3.org/2002/07/owl#onDataRange",
        "http://www.w3.org/2002/07/owl#onDatatype",
        "http://www.w3.org/2002/07/owl#oneOf",
        "http://www.w3.org/2002/07/owl#onProperties",
        "http://www.w3.org/2002/07/owl#onProperty",
        "http://www.w3.org/2002/07/owl#propertyChainAxiom",
        "http://www.w3.org/2002/07/owl#propertyDisjointWith",
        "http://www.w3.org/2002/07/owl#qualifiedCardinality",
        "http://www.w3.org/2002/07/owl#someValuesFrom",
        "http://www.w3.org/2002/07/owl#sourceIndividual",
        "http://www.w3.org/2002/07/owl#targetIndividual",
        "http://www.w3.org/2002/07/owl#targetValue",
        "http://www.w3.org/2002/07/owl#unionOf",
        "http://www.w3.org/2002/07/owl#withRestrictions"
    };

    public static final String[] INV_PROPERTIES = new String [] {
        "http://www.w3.org/2000/01/rdf-schema#subClassOf",
        "http://www.w3.org/2004/02/skos/core#broader",
        "http://www.w3.org/2004/02/skos/core#broaderTransitive"
    };
    
    private static Set<String> blackProps = null;
    private static Set<String> invProps = null;
    
    public static Set<String> blackProperties() {
        if(blackProps == null) {
            blackProps = new HashSet<>(Arrays.asList(BLACK_PROPERTIES));
        }
        return blackProps;
    }
    
    public static Set<String> invProperties() {
        if(invProps == null) {
            invProps = new HashSet<>(Arrays.asList(INV_PROPERTIES));
        }
        return invProps;
    }
    
    public final List<DataViewEntry> entries;

    public DataView(List<DataViewEntry> entries) {
        this.entries = entries;
    }

    public static DataView build(Dataset left, Dataset right, AlignmentSet alignment) {
        alignment.sortAlignments();
        Tree leftTrees = buildTree(left);
        List<Paths> rightTrees = convertToPaths(buildTree(right));
        return new DataView(convertToDataView(leftTrees, rightTrees, alignment, left, right));
    }

    static List<DataViewEntry> convertToDataView(Tree t, List<Paths> rightPaths, AlignmentSet alignmentSet, Dataset left, Dataset right) {
        List<Paths> ps = convertToPaths(t);
        List<DataViewEntry> dves = new ArrayList<>();
        for (Paths p : ps) {
            List<DataViewPath> l = new ArrayList<>();
            for (Alignment a : alignmentSet) {
                Resource entity1 = a.entity1.toJena(left), entity2 = a.entity2.toJena(right);


                List<String> leftPath = p.paths.get(entity1);
                if (leftPath == null) {
                    continue;
                }
                List<String> rightPath = null;
                String rightRoot = null;
                for (Paths rightP : rightPaths) {
                    if (rightP.paths.containsKey(entity2)) {
                        rightPath = rightP.paths.get(entity2);
                        rightRoot = rightP.root.toString();
                        break;
                    }
                }
                if (rightRoot == null) {
                    rightRoot = "";
                    rightPath = Collections.EMPTY_LIST;
                }
                l.add(new DataViewPath(rightRoot, leftPath, rightPath, a));
            }
            dves.add(new DataViewEntry(p.root.toString(), l));
        }
        return dves;
    }

    private static class Roots {
        Map<Resource, Set<Resource>> roots = new HashMap<>();
        Map<Resource, Set<Resource>> invRoots = new HashMap<>();
        
        public boolean containsKey(Resource r) { return roots.containsKey(r); }
        public Collection<Set<Resource>> values() { return roots.values(); }
        public Set<Resource> get(Resource r) { return roots.get(r); }
        public void put(Resource r, Resource r2) {
            if(!roots.containsKey(r)) {
                roots.put(r, new HashSet<>());
            }
            roots.get(r).add(r2);
            if(!invRoots.containsKey(r2)) {
                invRoots.put(r2, new HashSet<>());
            }
            invRoots.get(r2).add(r);
        }
        public Set<Resource> getInv(Resource r) { return invRoots.containsKey(r) ? new HashSet<>(invRoots.get(r)) : Collections.EMPTY_SET; }
        public void remove(Resource r, Resource r2) {
            if(roots.containsKey(r))
                roots.get(r).remove(r2);
            if(invRoots.containsKey(r2))
                invRoots.get(r2).remove(r);
        }
        public void addAll(Resource r, Set<Resource> r2s) {
            for(Resource r2 : r2s) {
                put(r, r2);
            }
        }

        @Override
        public String toString() {
            return "Roots{" +
                    "roots=" + roots +
                    ", invRoots=" + invRoots +
                    '}';
        }
    }
    
    static Set<Resource> findRoots(Dataset d) {
        Roots roots = new Roots();
        StmtIterator iter = d.listStatements();
        while (iter.hasNext()) {
            Statement s = iter.next();
            if(!s.getObject().isResource()) {
                if(!roots.containsKey(s.getSubject())) {
                    roots.put(s.getSubject(), s.getSubject());
                }
                continue;
            }
            if (!s.getObject().isResource() || s.getObject().asResource().equals(s.getSubject())
                    || blackProperties().contains(s.getPredicate().getURI())) {
                continue;
            }
            Resource subj = invProperties().contains(s.getPredicate().getURI()) ? s.getObject().asResource() : s.getSubject();
            Resource obj = invProperties().contains(s.getPredicate().getURI()) ? s.getSubject() : s.getObject().asResource();
            if (roots.containsKey(subj)) {
                if (roots.containsKey(obj) && roots.get(obj).contains(subj)) {
                    continue; // This would be a loop
                }
                for(Resource r : roots.getInv(obj)) {
                    roots.remove(r, obj);
                    roots.addAll(r, roots.get(subj));
                }
            } else {
                for(Resource r : roots.getInv(obj)) {
                    roots.remove(r, obj);
                    roots.put(r, subj);
                }
                roots.put(obj, subj);
            }
        }
        return roots.values().stream().flatMap(x -> x.stream()).collect(Collectors.toSet());
    }

    static Tree buildTree(Dataset d) {
        Set<Resource> roots = findRoots(d);
        Set<Resource> visited = new HashSet<>(roots);
        Map<Resource, List<Statement>> adjacencies = new HashMap<>();
        for (Resource root : roots) {
            breadthSearch(root, adjacencies, d, visited);
        }
        return new Tree(adjacencies, roots);
    }

    static void breadthSearch(Resource current, Map<Resource, List<Statement>> adjacencies, Dataset d, Set<Resource> visited) {
        StmtIterator iter = d.listStatements(current, null, null);
        adjacencies.put(current, new ArrayList<>());
        List<Resource> toVisit = new ArrayList<>();
        while (iter.hasNext()) {
            Statement s = iter.next();
            if (!s.getObject().isResource() || s.getObject().asResource().equals(s.getSubject())
                    || blackProperties().contains(s.getPredicate().getURI())
                    || invProperties().contains(s.getPredicate().getURI())) {
                continue;
            }
            Resource o = s.getObject().asResource();
            if (visited.contains(o)) {
                continue;
            }
            adjacencies.get(current).add(s);
            visited.add(o);
            toVisit.add(o);
        }
        iter = d.listStatements(null, null, current);
        while (iter.hasNext()) {
            Statement s = iter.next();
            if (!s.getObject().isResource() || s.getObject().asResource().equals(s.getSubject())
                    || blackProperties().contains(s.getPredicate().getURI())
                    || !invProperties().contains(s.getPredicate().getURI())) {
                continue;
            }
            Resource o = s.getSubject();
            if (visited.contains(o)) {
                continue;
            }
            adjacencies.get(current).add(s.getModel().createStatement(current, s.getPredicate(), o));
            visited.add(o);
            toVisit.add(o);
        }
        for (Resource o : toVisit) {
            breadthSearch(o, adjacencies, d, visited);
        }
    }

    static List<Paths> convertToPaths(Tree t) {
        ArrayList<Paths> p = new ArrayList<>();
        for (Resource root : t.roots) {
            Map<Resource, List<String>> paths = new HashMap<>();
            paths.put(root, new ArrayList<>());
            buildPaths(paths, t, root, new ArrayList<>());
            p.add(new Paths(root, paths));
        }
        return p;
    }

    static void buildPaths(Map<Resource, List<String>> paths, Tree t, Resource n, List<String> currentPath) {

        for (Statement s : t.adjacencies.get(n)) {
            if (!paths.containsKey(s.getObject().asResource())) {
                List<String> l = new ArrayList<>(currentPath);
                final String path;
                if(invProperties().contains(s.getPredicate().getURI())) {
                    path = s.getPredicate().getURI() + " -";
                } else {
                    path = s.getPredicate().getURI();
                }
                l.add(path);
                paths.put(s.getObject().asResource(), l);
                buildPaths(paths, t, s.getObject().asResource(), l);
            }
        }
    }

    static class Tree {

        Map<Resource, List<Statement>> adjacencies;
        Set<Resource> roots;

        public Tree(Map<Resource, List<Statement>> adjacencies, Set<Resource> roots) {
            this.adjacencies = adjacencies;
            this.roots = roots;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + Objects.hashCode(this.adjacencies);
            hash = 29 * hash + Objects.hashCode(this.roots);
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
            final Tree other = (Tree) obj;
            if (!Objects.equals(this.adjacencies, other.adjacencies)) {
                return false;
            }
            if (!Objects.equals(this.roots, other.roots)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Tree{" + "adjacencies=" + adjacencies + ", roots=" + roots + '}';
        }

    }

    static class Paths {

        Resource root;
        Map<Resource, List<String>> paths;

        public Paths(Resource root, Map<Resource, List<String>> paths) {
            this.root = root;
            this.paths = paths;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.root);
            hash = 37 * hash + Objects.hashCode(this.paths);
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
            final Paths other = (Paths) obj;
            if (!Objects.equals(this.root, other.root)) {
                return false;
            }
            if (!Objects.equals(this.paths, other.paths)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Paths{" + "root=" + root + ", paths=" + paths + '}';
        }

    }

    public static class DataViewEntry {

        public String root;
        public List<DataViewPath> paths;

        public DataViewEntry(String root, List<DataViewPath> paths) {
            this.root = root;
            this.paths = paths;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + Objects.hashCode(this.root);
            hash = 97 * hash + Objects.hashCode(this.paths);
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
            final DataViewEntry other = (DataViewEntry) obj;
            if (!Objects.equals(this.root, other.root)) {
                return false;
            }
            if (!Objects.equals(this.paths, other.paths)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "DataViewEntry{" + "root=" + root + ", paths=" + paths + '}';
        }

    }

    public static class DataViewPath {

        public String rightRoot;
        public List<String> leftPath;
        public List<String> rightPath;
        public Alignment alignment;

        public DataViewPath(String rightRoot, List<String> leftPath, List<String> rightPath, Alignment alignment) {
            this.rightRoot = rightRoot;
            this.leftPath = leftPath;
            this.rightPath = rightPath;
            this.alignment = alignment;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Objects.hashCode(this.rightRoot);
            hash = 83 * hash + Objects.hashCode(this.leftPath);
            hash = 83 * hash + Objects.hashCode(this.rightPath);
            hash = 83 * hash + Objects.hashCode(this.alignment);
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
            final DataViewPath other = (DataViewPath) obj;
            if (!Objects.equals(this.rightRoot, other.rightRoot)) {
                return false;
            }
            if (!Objects.equals(this.leftPath, other.leftPath)) {
                return false;
            }
            if (!Objects.equals(this.rightPath, other.rightPath)) {
                return false;
            }
            if (!Objects.equals(this.alignment, other.alignment)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "DataViewPath{" + "rightRoot=" + rightRoot + ", leftPath=" + leftPath + ", rightPath=" + rightPath + ", alignment=" + alignment + '}';
        }

    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.entries);
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
        final DataView other = (DataView) obj;
        if (!Objects.equals(this.entries, other.entries)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "DataView{" + "entries=" + entries + '}';
    }
}
