package org.insightcentre.uld.naisc.analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.lens.Label;
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader.ModelDataset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class LabelAnalysisTest {

    public LabelAnalysisTest() {
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
     * Test of analyse method, of class LabelAnalysis.
     */
    @Test
    public void testAnalyse() {
       System.out.println("analyse");
        Model model = ModelFactory.createDefaultModel();
        Map<String, Object> params = new HashMap<>();
        final Resource res = model.createResource("http://www.example.com/foo");
        final Resource res2 = model.createResource("http://www.example.com/foo2");
        final Resource res3 = model.createResource("http://www.example.com/foo3");
        final Resource res4 = model.createResource("http://www.example.com/foo4");
        final Resource res5 = model.createResource("http://www.example.com/foo5");
        
        model.add(res, 
                model.createProperty(Label.RDFS_LABEL), 
                model.createLiteral("english", "en"));
        
        model.add(res2, 
                model.createProperty(Label.RDFS_LABEL), 
                model.createLiteral("deutsch", "de"));
        
        model.add(res3, 
                model.createProperty(Label.RDFS_LABEL), 
                model.createLiteral("???"));
        
        model.add(res4, 
                model.createProperty(Label.RDFS_LABEL), 
                model.createLiteral("more english", "en"));
        
        model.add(res5, 
                model.createProperty(Label.SKOS_PREFLABEL), 
                model.createLiteral("???"));
       
        DatasetAnalyzer instance = new DatasetAnalyzer();
        List<LabelResult> result = instance.analyseModel(new ModelDataset(model, "model"), new ModelDataset(model, "model")).leftLabels;
        assert(result.stream().anyMatch(x -> x.uri.equals("")));
        assert(result.stream().anyMatch(x -> x.uri.equals(Label.RDFS_LABEL)));
        assert(result.stream().anyMatch(x -> x.uri.equals(Label.SKOS_PREFLABEL)));
    }

    String[] naturalStrings = new String[] {
            "Itk mutant",
"mCD14 ligands",
"sterol",
"RAR alpha agonist",
"IRE",
"AP-1 specificity",
"NF-kappaB subunits",
"liquid culture",
"human myeloid cell nuclear",
"minimal beta-globin promoter",
"alpha-globin chains",
"diseases",
"pCD41 trans-activation",
"in the TF site",
"different variants",
"In vitro translated hGR",
"effector function",
"FITC-LPS",
"c-sis/platelet-derived growth factor-B (PDGF-B) proto-oncogene",
//"nt 465 to 720",
"induction",
"carboxyl-",
"IL4 enhancer",
"enhancer activation",
"NF-MATp35",
"successive stages of commitment and differentiation",
"NF-kappa B/Rel homodimer",
"5' flanking region",
"multiple molecular partners",
"former",
"calcium- and calmodulin-dependent phosphatase calcineurin",
//"-510",
"cAMP response",
"hMRP8 promoter element",
"other regions of",
"Progesterone",
"pp52 promoter",
"U2",
"growth",
"GM-CSF transcription",
"NFAT kinase",
"deletion mutants of the CD14 5' upstream sequence coupled to a reporter gene construct",
"atherosclerotic plaque development",
"early B-cell development",
"vertebrates",
"of monocytes"
            };
    
    /**
     * Test of isNaturalLangLike method, of class LabelAnalysis.
     */
    @Test
    public void testIsNaturalLangLike() {
        System.out.println("isNaturalLangLike");
        DatasetAnalyzer instance = new DatasetAnalyzer();
        for(String naturalString : naturalStrings) {
            System.err.println(naturalString);
            assert(instance.isNaturalLangLike(naturalString));
        }
    }

    /**
     * Test of diversity method, of class LabelAnalysis.
     */
    @Test
    public void testDiversity() {
        System.out.println("diversity");
        List<String> notDiverse = Arrays.asList("foo","foo","foo","foo","foo","foo","foo","foo","foo","foo","foo","foo","foo","foo","foo","foo");
        List<String> tooDiverse = Arrays.asList("foo1","foo2","foo3","foo4","foo5","foo6","foo7","foo8","foo9","fooa","foob","fooc","food","fooe","foof","foog");
        List<String> okay = Arrays.asList("foo","foo","foo","bar","bar","bar","bar","bar","foo","foo","baz","baz","baz","baz","baz","baz");
        DatasetAnalyzer instance = new DatasetAnalyzer();
        double d1 = instance.diversity(notDiverse);
        double d2 = instance.diversity(tooDiverse);
        double d3 = instance.diversity(okay);
        assert(d3 > d1);
        assert(d3 > d2);
    }

}