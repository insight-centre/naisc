package org.insightcentre.uld.naisc.meas;

import java.util.ArrayList;
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

    public final List<DataViewEntry> entries;

    public DataView(List<DataViewEntry> entries) {
        this.entries = entries;
    }

    public static DataView build(Dataset left, Dataset right, AlignmentSet alignment) {
        alignment.sortAlignments();
        Tree leftTrees = buildTree(left);
        List<Paths> rightTrees = convertToPaths(buildTree(right));
        return new DataView(convertToDataView(leftTrees, rightTrees, alignment));
    }

    static List<DataViewEntry> convertToDataView(Tree t, List<Paths> rightPaths, AlignmentSet alignmentSet) {
        List<Paths> ps = convertToPaths(t);
        List<DataViewEntry> dves = new ArrayList<>();
        for (Paths p : ps) {
            List<DataViewPath> l = new ArrayList<>();
            for (Alignment a : alignmentSet) {
                
                List<String> leftPath = p.paths.get(a.entity1);
                if(leftPath == null)
                    continue;
                List<String> rightPath = null;
                String rightRoot = null;
                for (Paths rightP : rightPaths) {
                    if (rightP.paths.containsKey(a.entity2)) {
                        rightPath = rightP.paths.get(a.entity2);
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

    static Set<Resource> findRoots(Dataset d) {
        Map<Resource, Set<Resource>> roots = new HashMap<>();
        StmtIterator iter = d.listStatements();
        while (iter.hasNext()) {
            Statement s = iter.next();
            if (!s.getObject().isResource() || s.getObject().asResource().equals(s.getSubject())) {
                continue;
            }
            Resource o = s.getObject().asResource();
            if(roots.containsKey(s.getSubject())) {
                if(roots.containsKey(o) && roots.get(o).contains(s.getSubject())) 
                    continue; // This would be a loop
                for(Map.Entry<Resource, Set<Resource>> e : roots.entrySet()) {
                    if(e.getValue().contains(o)) {
                        e.getValue().remove(o);
                        e.getValue().addAll(roots.get(s.getSubject()));
                        if(e.getValue().contains(e.getKey()))
                            throw new RuntimeException("Root algorithm is bust");
                    }
                }
            } else {
                for(Map.Entry<Resource, Set<Resource>> e : roots.entrySet()) {
                    if(e.getValue().contains(o)) {
                        e.getValue().remove(o);
                        e.getValue().add(s.getSubject());
                        if(e.getValue().contains(e.getKey()))
                            throw new RuntimeException("Root algorithm is bust");
                    }
                }
                if(!roots.containsKey(o)) {
                    roots.put(o, new HashSet<>());
                }
                roots.get(o).add(s.getSubject());
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
            if (!s.getObject().isResource() || s.getObject().asResource().equals(s.getSubject())) {
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
                l.add(s.getPredicate().getURI());
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
