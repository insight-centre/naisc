package org.insightcentre.uld.naisc.constraint;

import org.apache.jena.vocabulary.SKOS;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.URIRes;
import org.junit.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TaxonomicTest {


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

    @Test
    public void testTaxonomic() {
        Constraint taxonomic = new Taxonomic().make(Collections.EMPTY_MAP);

        List<Alignment> valids = Arrays.asList(
                new Alignment(new URIRes("file:uri1", "ds"), new URIRes("file:uri1", "ds"), 0.5, SKOS.exactMatch.getURI(), null),
                new Alignment(new URIRes("file:uri2a", "ds"), new URIRes("file:uri2", "ds"), 0.5, SKOS.broadMatch.getURI(), null),
                new Alignment(new URIRes("file:uri2b", "ds"), new URIRes("file:uri2", "ds"), 0.5, SKOS.broadMatch.getURI(), null),
                new Alignment(new URIRes("file:uri3", "ds"), new URIRes("file:uri3a", "ds"), 0.5, SKOS.narrowMatch.getURI(), null),
                new Alignment(new URIRes("file:uri3", "ds"), new URIRes("file:uri3b", "ds"), 0.5, SKOS.narrowMatch.getURI(), null),
                new Alignment(new URIRes("file:uri4", "ds"), new URIRes("file:uri4", "ds"), 0.5, SKOS.relatedMatch.getURI(), null),
                new Alignment(new URIRes("file:uri5", "ds"), new URIRes("file:uri5", "ds"), 0.5, SKOS.relatedMatch.getURI(), null),
                new Alignment(new URIRes("file:uri4", "ds"), new URIRes("file:uri5", "ds"), 0.5, SKOS.relatedMatch.getURI(), null)
        );

        List<Alignment> invalids = Arrays.asList(
                new Alignment(new URIRes("file:uri1", "ds"), new URIRes("file:uri2", "ds"), 0.5, SKOS.exactMatch.getURI(), null),
                new Alignment(new URIRes("file:uri2a", "ds"), new URIRes("file:uri3", "ds"), 0.5, SKOS.broadMatch.getURI(), null),
                new Alignment(new URIRes("file:uri1", "ds"), new URIRes("file:uri4", "ds"), 0.5, SKOS.relatedMatch.getURI(), null)
        );

        for(Alignment a : valids) {
            System.err.println(a);
            assert(taxonomic.canAdd(a));
            taxonomic.add(a);
        }
        for(Alignment a : invalids) {
            assert(!taxonomic.canAdd(a));
        }
    }

}
