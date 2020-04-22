package org.insightcentre.uld.naisc.meas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader;
import org.insightcentre.uld.naisc.meas.DataView.DataViewPath;
import org.insightcentre.uld.naisc.util.EdmondsChuLiu;
import org.insightcentre.uld.naisc.util.EdmondsChuLiu.Node;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author John McCrae
 */
public class DataViewTest {

    public DataViewTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of build method, of class DataView.
     */
    @Test
    public void testBuild() {
        System.out.println("build");
        Model mleft = ModelFactory.createDefaultModel();
        mleft.add(mleft.createResource("file:foo/id1"), mleft.createProperty("file:p1"), mleft.createResource("file:foo/id2"));
        mleft.add(mleft.createResource("file:foo/id1"), mleft.createProperty("file:p2"), mleft.createResource("file:foo/id3"));
        mleft.add(mleft.createResource("file:foo/id1"), mleft.createProperty("file:p1"), mleft.createResource("file:foo/id4"));
        mleft.add(mleft.createResource("file:foo/id5"), mleft.createProperty("file:p1"), mleft.createResource("file:foo/id6"));
        Model mright = ModelFactory.createDefaultModel();
        mright.add(mright.createResource("file:bar/id1"), mright.createProperty("file:p1"), mright.createResource("file:bar/id2"));
        mright.add(mright.createResource("file:bar/id1"), mright.createProperty("file:p2"), mright.createResource("file:bar/id3"));
        mright.add(mright.createResource("file:bar/id5"), mright.createProperty("file:p1"), mright.createResource("file:bar/id4"));
        mright.add(mright.createResource("file:bar/id5"), mright.createProperty("file:p1"), mright.createResource("file:bar/id6"));
        Dataset left = new DefaultDatasetLoader.ModelDataset(mleft, "mleft");
        Dataset right = new DefaultDatasetLoader.ModelDataset(mright, "mright");
        AlignmentSet alignment = new AlignmentSet();
        alignment.add(new Alignment(new URIRes("file:foo/id1", "mleft"), new URIRes("file:bar/id1", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignment.add(new Alignment(new URIRes("file:foo/id2", "mleft"), new URIRes("file:bar/id2", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignment.add(new Alignment(new URIRes("file:foo/id3", "mleft"), new URIRes("file:bar/id3", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignment.add(new Alignment(new URIRes("file:foo/id4", "mleft"), new URIRes("file:bar/id4", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignment.add(new Alignment(new URIRes("file:foo/id5", "mleft"), new URIRes("file:bar/id5", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignment.add(new Alignment(new URIRes("file:foo/id6", "mleft"), new URIRes("file:bar/id6", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        DataView expResult = new DataView(new ArrayList<DataView.DataViewEntry>() {
            {
                add(new DataView.DataViewEntry("file:foo/id1", new ArrayList<DataViewPath>() {
                    {
                        add(new DataViewPath("file:bar/id1", Collections.EMPTY_LIST, Collections.EMPTY_LIST, new Alignment(new URIRes("file:foo/id1", "mleft"), new URIRes("file:bar/id1", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null)));
                        add(new DataViewPath("file:bar/id1", Arrays.asList("file:p1"), Arrays.asList("file:p1"), new Alignment(new URIRes("file:foo/id2", "mleft"), new URIRes("file:bar/id2", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null)));
                        add(new DataViewPath("file:bar/id1", Arrays.asList("file:p2"), Arrays.asList("file:p2"), new Alignment(new URIRes("file:foo/id3", "mleft"), new URIRes("file:bar/id3", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null)));
                        add(new DataViewPath("file:bar/id5", Arrays.asList("file:p1"), Arrays.asList("file:p1"), new Alignment(new URIRes("file:foo/id4", "mleft"), new URIRes("file:bar/id4", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null)));
                    }
                }));
                add(new DataView.DataViewEntry("file:foo/id5", new ArrayList<DataViewPath>() {
                    {
                        add(new DataViewPath("file:bar/id5", Arrays.asList(), Arrays.asList(), new Alignment(new URIRes("file:foo/id5", "mleft"), new URIRes("file:bar/id5", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null)));
                        add(new DataViewPath("file:bar/id5", Arrays.asList("file:p1"), Arrays.asList("file:p1"), new Alignment(new URIRes("file:foo/id6", "mleft"), new URIRes("file:bar/id6", "mright"), 1.0, Alignment.SKOS_EXACT_MATCH, null)));
                    }
                }));
            }
        });
        DataView result = DataView.build(left, right, alignment);
        assertEquals(expResult.entries.size(), result.entries.size());
        for(int i = 0; i < expResult.entries.size(); i++) {
            assertEquals(expResult.entries.get(i).root, result.entries.get(i).root);
            assertEquals(expResult.entries.get(i).paths.size(), result.entries.get(i).paths.size());
            for(int j = 0; j < expResult.entries.get(i).paths.size(); j++) {
                System.err.println(i + "," + j);
                System.err.println(expResult.entries.get(i).paths.get(j));
                System.err.println(result.entries.get(i).paths.get(j));
                System.err.println(expResult.entries.get(i).paths.get(j).equals(result.entries.get(i).paths.get(j)));
                assertEquals(expResult.entries.get(i).paths.get(j), result.entries.get(i).paths.get(j));    
            }
        }
    }

    /**
     * Test of convertToDataView method, of class DataView.
     */
    @Test
    public void testConvertToDataView() {
        System.out.println("convertToDataView");
        Model model = ModelFactory.createDefaultModel();
        Map<Resource, List<Statement>> adjacencies = new HashMap<>();
        adjacencies.put(model.createResource("file:foo/id1"), Arrays.asList(model.createStatement(model.createResource("file:foo/id1"), model.createProperty("file:p1"), model.createResource("file:foo/id2"))));
        adjacencies.put(model.createResource("file:foo/id2"), Arrays.asList(model.createStatement(model.createResource("file:foo/id2"), model.createProperty("file:p1"), model.createResource("file:foo/id4"))));
        adjacencies.put(model.createResource("file:foo/id3"), Arrays.asList(model.createStatement(model.createResource("file:foo/id3"), model.createProperty("file:p1"), model.createResource("file:foo/id1"))));
        adjacencies.put(model.createResource("file:foo/id4"), Arrays.asList(model.createStatement(model.createResource("file:foo/id4"), model.createProperty("file:p1"), model.createResource("file:foo/id6"))));
        adjacencies.put(model.createResource("file:foo/id6"), Arrays.asList(model.createStatement(model.createResource("file:foo/id6"), model.createProperty("file:p1"), model.createResource("file:foo/id5"))));
        adjacencies.put(model.createResource("file:foo/id5"), Arrays.asList());
        Set<Resource> roots = new HashSet<>();
        roots.add(model.createResource("file:foo/id3"));
        
        DataView.Tree t = new DataView.Tree(adjacencies, roots);
        Resource root = model.createResource("file:bar/id1");
        Map<Resource, List<String>> paths = new HashMap<>();
        paths.put(model.createResource("file:bar/id1"), Arrays.asList("file:p1"));
        paths.put(model.createResource("file:bar/id2"), Arrays.asList("file:p1","file:p2"));
        paths.put(model.createResource("file:bar/id3"), Arrays.asList("file:p3"));
        paths.put(model.createResource("file:bar/id4"), Arrays.asList("file:p4"));
        paths.put(model.createResource("file:bar/id5"), Arrays.asList("file:p1"));
        paths.put(model.createResource("file:bar/id6"), Arrays.asList());
        List<DataView.Paths> rightPaths = Collections.singletonList(new DataView.Paths(root, paths));
        AlignmentSet alignmentSet = new AlignmentSet();
        alignmentSet.add(new Alignment(new URIRes("file:foo/id1", "model"), new URIRes("file:bar/id1", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignmentSet.add(new Alignment(new URIRes("file:foo/id2", "model"), new URIRes("file:bar/id2", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignmentSet.add(new Alignment(new URIRes("file:foo/id3", "model"), new URIRes("file:bar/id3", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignmentSet.add(new Alignment(new URIRes("file:foo/id4", "model"), new URIRes("file:bar/id4", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignmentSet.add(new Alignment(new URIRes("file:foo/id5", "model"), new URIRes("file:bar/id5", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        alignmentSet.add(new Alignment(new URIRes("file:foo/id6", "model"), new URIRes("file:bar/id6", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null));
        DataView.DataViewEntry expResult = new DataView.DataViewEntry("file:foo/id3", Arrays.asList(
                new DataViewPath("file:bar/id1", Arrays.asList("file:p1"), Arrays.asList("file:p1"), new Alignment(new URIRes("file:foo/id1", "model"), new URIRes("file:bar/id1", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null)),
                new DataViewPath("file:bar/id1", Arrays.asList("file:p1","file:p1"), Arrays.asList("file:p1","file:p2"), new Alignment(new URIRes("file:foo/id2", "model"), new URIRes("file:bar/id2", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null)),
                new DataViewPath("file:bar/id1", Arrays.asList(), Arrays.asList("file:p3"), new Alignment(new URIRes("file:foo/id3", "model"), new URIRes("file:bar/id3", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null)),
                new DataViewPath("file:bar/id1", Arrays.asList("file:p1","file:p1","file:p1"), Arrays.asList("file:p4"), new Alignment(new URIRes("file:foo/id4", "model"), new URIRes("file:bar/id4", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null)),
                new DataViewPath("file:bar/id1", Arrays.asList("file:p1","file:p1","file:p1","file:p1","file:p1"), Arrays.asList("file:p1"), new Alignment(new URIRes("file:foo/id5", "model"), new URIRes("file:bar/id5", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null)),
                new DataViewPath("file:bar/id1", Arrays.asList("file:p1","file:p1","file:p1","file:p1"), Arrays.asList(), new Alignment(new URIRes("file:foo/id6", "model"), new URIRes("file:bar/id6", "model"), 1.0, Alignment.SKOS_EXACT_MATCH, null))
        ));
        List<DataView.DataViewEntry> result = DataView.convertToDataView(t, rightPaths, alignmentSet, new DefaultDatasetLoader.ModelDataset(model, "model"), new DefaultDatasetLoader.ModelDataset(model, "model"));
        assertEquals(1, result.size());
        assertEquals(expResult.root, result.get(0).root);
        for(int i = 0; i < expResult.paths.size(); i++) {
            assertEquals(expResult.paths.get(i), result.get(0).paths.get(i));
        }
        
    }

    /**
     * Test of buildTree method, of class DataView.
     */
    @Test
    public void testBuildTree() {
        System.out.println("buildTree");
        Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource("file:foo/id1"), model.createProperty("file:p1"), model.createResource("file:foo/id2"));
        model.add(model.createResource("file:foo/id2"), model.createProperty("file:p2"), model.createResource("file:foo/id3"));
        model.add(model.createResource("file:foo/id2"), model.createProperty("file:p1"), model.createResource("file:foo/id4"));
        model.add(model.createResource("file:foo/id3"), model.createProperty("file:p1"), model.createResource("file:foo/id1"));
        model.add(model.createResource("file:foo/id4"), model.createProperty("file:p1"), model.createResource("file:foo/id6"));
        model.add(model.createResource("file:foo/id6"), model.createProperty("file:p1"), model.createResource("file:foo/id5"));
        model.add(model.createResource("file:foo/id4"), model.createProperty("file:p1"), model.createResource("file:foo/id4"));
        Dataset d = new DefaultDatasetLoader.ModelDataset(model, "d");
        
                Map<Resource, List<Statement>> adjacencies = new HashMap<>();
        adjacencies.put(model.createResource("file:foo/id1"), Arrays.asList(model.createStatement(model.createResource("file:foo/id1"), model.createProperty("file:p1"), model.createResource("file:foo/id2"))));
        adjacencies.put(model.createResource("file:foo/id2"), Arrays.asList(model.createStatement(model.createResource("file:foo/id2"), model.createProperty("file:p1"), model.createResource("file:foo/id4"))));
        adjacencies.put(model.createResource("file:foo/id3"), Arrays.asList(model.createStatement(model.createResource("file:foo/id3"), model.createProperty("file:p1"), model.createResource("file:foo/id1"))));
        adjacencies.put(model.createResource("file:foo/id4"), Arrays.asList(model.createStatement(model.createResource("file:foo/id4"), model.createProperty("file:p1"), model.createResource("file:foo/id6"))));
        adjacencies.put(model.createResource("file:foo/id6"), Arrays.asList(model.createStatement(model.createResource("file:foo/id6"), model.createProperty("file:p1"), model.createResource("file:foo/id5"))));
        adjacencies.put(model.createResource("file:foo/id5"), Arrays.asList());
        Set<Resource> roots = new HashSet<>();
        roots.add(model.createResource("file:foo/id3"));
        DataView.Tree expResult = new DataView.Tree(adjacencies, roots);
        DataView.Tree result = DataView.buildTree(d);
        assertEquals(expResult, result);
    }

    /**
     * Test of convertToPaths method, of class DataView.
     */
    @Test
    public void testConvertToPaths() {
        System.out.println("convertToPaths");
        Model model = ModelFactory.createDefaultModel();        
        Map<Resource, List<Statement>> adjacencies = new HashMap<>();
        Set<Resource> roots = new HashSet<>();
        adjacencies.put(model.createResource("file:foo/id1"), Arrays.asList(model.createStatement(model.createResource("file:foo/id1"), model.createProperty("file:p1"), model.createResource("file:foo/id2"))));
        adjacencies.put(model.createResource("file:foo/id2"), Arrays.asList(model.createStatement(model.createResource("file:foo/id2"), model.createProperty("file:p2"), model.createResource("file:foo/id3")),model.createStatement(model.createResource("file:foo/id2"), model.createProperty("file:p5"), model.createResource("file:foo/id4"))));
        adjacencies.put(model.createResource("file:foo/id3"), Arrays.asList());
        adjacencies.put(model.createResource("file:foo/id4"), Arrays.asList(model.createStatement(model.createResource("file:foo/id4"), model.createProperty("file:p3"), model.createResource("file:foo/id5"))));
        adjacencies.put(model.createResource("file:foo/id5"), Arrays.asList(model.createStatement(model.createResource("file:foo/id5"), model.createProperty("file:p4"), model.createResource("file:foo/id6"))));
        adjacencies.put(model.createResource("file:foo/id6"), Arrays.asList());
        Resource n = model.createResource("file:foo/id1");
        roots.add(n);
        DataView.Tree t = new DataView.Tree(adjacencies, roots);

        Map<Resource, List<String>> paths = new HashMap<>();
        paths.put(model.createResource("file:foo/id1"), Arrays.asList());
        paths.put(model.createResource("file:foo/id2"), Arrays.asList("file:p1"));
        paths.put(model.createResource("file:foo/id3"), Arrays.asList("file:p1","file:p2"));
        paths.put(model.createResource("file:foo/id4"), Arrays.asList("file:p1","file:p5"));
        paths.put(model.createResource("file:foo/id5"), Arrays.asList("file:p1","file:p5","file:p3"));
        paths.put(model.createResource("file:foo/id6"), Arrays.asList("file:p1","file:p5","file:p3","file:p4"));
        List<DataView.Paths> expResult = Arrays.asList(new DataView.Paths(model.createResource("file:foo/id1"), paths));
        List<DataView.Paths> result = DataView.convertToPaths(t);
        assertEquals(expResult, result);
    }

    /**
     * Test of buildPaths method, of class DataView.
     */
    @Test
    public void testBuildPaths() {
        System.out.println("buildPaths");
        Model model = ModelFactory.createDefaultModel();
        Map<Resource, List<Statement>> adjacencies = new HashMap<>();
        Set<Resource> roots = new HashSet<>();
        adjacencies.put(model.createResource("file:foo/id1"), Arrays.asList(model.createStatement(model.createResource("file:foo/id1"), model.createProperty("file:p1"), model.createResource("file:foo/id2"))));
        adjacencies.put(model.createResource("file:foo/id2"), Arrays.asList(model.createStatement(model.createResource("file:foo/id2"), model.createProperty("file:p2"), model.createResource("file:foo/id3")),model.createStatement(model.createResource("file:foo/id2"), model.createProperty("file:p5"), model.createResource("file:foo/id4"))));
        adjacencies.put(model.createResource("file:foo/id3"), Arrays.asList());
        adjacencies.put(model.createResource("file:foo/id4"), Arrays.asList(model.createStatement(model.createResource("file:foo/id4"), model.createProperty("file:p3"), model.createResource("file:foo/id5"))));
        adjacencies.put(model.createResource("file:foo/id5"), Arrays.asList(model.createStatement(model.createResource("file:foo/id5"), model.createProperty("file:p4"), model.createResource("file:foo/id6"))));
        adjacencies.put(model.createResource("file:foo/id6"), Arrays.asList());
        Resource n = model.createResource("file:foo/id1");
        roots.add(n);
        DataView.Tree t = new DataView.Tree(adjacencies, roots);
        List<String> currentPath = new ArrayList<>();
        Map<Resource, List<String>> paths = new HashMap<>();
        DataView.buildPaths(paths, t, n, currentPath);
        Map<Resource, List<String>> expResult = new HashMap<>();
        expResult.put(model.createResource("file:foo/id2"), Arrays.asList("file:p1"));
        expResult.put(model.createResource("file:foo/id3"), Arrays.asList("file:p1","file:p2"));
        expResult.put(model.createResource("file:foo/id4"), Arrays.asList("file:p1","file:p5"));
        expResult.put(model.createResource("file:foo/id5"), Arrays.asList("file:p1","file:p5","file:p3"));
        expResult.put(model.createResource("file:foo/id6"), Arrays.asList("file:p1","file:p5","file:p3","file:p4"));

        for(Resource r : paths.keySet()) {
            //System.err.println(expResult.get(r) + " == " + paths.get(r));
            assertEquals(expResult.get(r), paths.get(r));
            
        }
        assertEquals(expResult.size(), paths.size());
    }

    /**
     * Test of findRoots method, of class DataView.
     */
    @Test
    public void testFindRoots() {
        System.out.println("findRoots");
        Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource("file:foo/id1"), model.createProperty("file:p1"), model.createResource("file:foo/id2"));
        model.add(model.createResource("file:foo/id2"), model.createProperty("file:p2"), model.createResource("file:foo/id3"));
        model.add(model.createResource("file:foo/id2"), model.createProperty("file:p1"), model.createResource("file:foo/id4"));
        model.add(model.createResource("file:foo/id1"), model.createProperty("file:p1"), model.createResource("file:foo/id3"));
        model.add(model.createResource("file:foo/id4"), model.createProperty("file:p1"), model.createResource("file:foo/id6"));
        model.add(model.createResource("file:foo/id6"), model.createProperty("file:p1"), model.createResource("file:foo/id5"));
        model.add(model.createResource("file:foo/id4"), model.createProperty("file:p1"), model.createResource("file:foo/id4"));
        Dataset d = new DefaultDatasetLoader.ModelDataset(model, "d");
        Set<Resource> expResult = new HashSet<>();
        expResult.add(model.createResource("file:foo/id1"));
        Set<Resource> result = DataView.findRoots(d);
        assertEquals(expResult, result);
    }

    /**
     * Test of breadthSearch method, of class DataView.
     */
    @Test
    public void testBreadthSearch() {
        System.out.println("breadthSearch");
        Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource("file:foo/id1"), model.createProperty("file:p1"), model.createResource("file:foo/id2"));
        model.add(model.createResource("file:foo/id2"), model.createProperty("file:p2"), model.createResource("file:foo/id3"));
        model.add(model.createResource("file:foo/id2"), model.createProperty("file:p1"), model.createResource("file:foo/id4"));
        model.add(model.createResource("file:foo/id3"), model.createProperty("file:p1"), model.createResource("file:foo/id1"));
        model.add(model.createResource("file:foo/id4"), model.createProperty("file:p1"), model.createResource("file:foo/id6"));
        model.add(model.createResource("file:foo/id6"), model.createProperty("file:p1"), model.createResource("file:foo/id5"));
        model.add(model.createResource("file:foo/id4"), model.createProperty("file:p1"), model.createResource("file:foo/id4"));
        Resource current = model.createResource("file:foo/id1");
        Map<Resource, List<Statement>> adjacencies = new HashMap<>();
        Dataset d = new DefaultDatasetLoader.ModelDataset(model, "d");
        Set<Resource> visited = new HashSet<>();
        visited.add(model.createResource("file:foo/id1"));
        DataView.breadthSearch(current, adjacencies, d, visited);
        Map<Resource, List<Statement>> expResult = new HashMap<>();
        expResult.put(model.createResource("file:foo/id1"), Arrays.asList(model.createStatement(model.createResource("file:foo/id1"), model.createProperty("file:p1"), model.createResource("file:foo/id2"))));
        expResult.put(model.createResource("file:foo/id2"), Arrays.asList(model.createStatement(model.createResource("file:foo/id2"), model.createProperty("file:p1"), model.createResource("file:foo/id4")),model.createStatement(model.createResource("file:foo/id2"), model.createProperty("file:p2"), model.createResource("file:foo/id3"))));
        expResult.put(model.createResource("file:foo/id3"), Arrays.asList());
        expResult.put(model.createResource("file:foo/id4"), Arrays.asList(model.createStatement(model.createResource("file:foo/id4"), model.createProperty("file:p1"), model.createResource("file:foo/id6"))));
        expResult.put(model.createResource("file:foo/id6"), Arrays.asList(model.createStatement(model.createResource("file:foo/id6"), model.createProperty("file:p1"), model.createResource("file:foo/id5"))));
        expResult.put(model.createResource("file:foo/id5"), Arrays.asList());
        for(Resource r : adjacencies.keySet()) {
            //System.err.println(expResult.get(r) + " == " + adjacencies.get(r));
            assertEquals(new HashSet<>(expResult.get(r)), new HashSet<>(adjacencies.get(r)));
        }
        assertEquals(expResult.size(), adjacencies.size());
    }
}
