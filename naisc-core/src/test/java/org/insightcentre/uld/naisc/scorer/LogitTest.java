package org.insightcentre.uld.naisc.scorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.FeatureSetWithScore;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.ScorerTrainer;
import org.insightcentre.uld.naisc.scorer.Logit.LogitFunction;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.StringPair;
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
public class LogitTest {

    public LogitTest() {
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
     * Test of id method, of class Logit.
     */
    @Test
    public void testId() {
        System.out.println("id");
        Logit instance = new Logit();
        String expResult = "logit";
        String result = instance.id();
        assertEquals(expResult, result);
    }

    /**
     * Test of makeScorer method, of class Logit.
     */
    @Test
    public void testMakeScorer() {
        System.out.println("makeScorer");
        Map<String, Object> params = new HashMap<>();
        Logit instance = new Logit();
        Scorer scorer = instance.makeScorer(params, null).get(0);
        FeatureSet fs = new FeatureSet(new StringPair[]{
            new StringPair("foo", "bar"),
            new StringPair("this", "that"),
            new StringPair("fizz", "buzz")
        }, new double[]{
            0.1, 0.8, 0.7
        }, "e1", "e2");
        double result = scorer.similarity(fs);
        assertEquals(result, 0.630, 0.001);
    }

    /**
     * Test of makeTrainer method, of class Logit.
     */
    @Test
    public void testMakeTrainer() {
        System.out.println("makeTrainer");
        Map<String, Object> params = new HashMap<>();
        String property = Alignment.SKOS_EXACT_MATCH;
        Logit instance = new Logit();
        Option<ScorerTrainer> trainer2 = instance.makeTrainer(params, property, null);
        assert (trainer2.has());
        ScorerTrainer trainer = trainer2.get();
        List<FeatureSetWithScore> data = new ArrayList<>();
        data.add(new FeatureSetWithScore(0.1, new StringPair[]{
            new StringPair("foo", "bar"),
            new StringPair("this", "that"),
            new StringPair("fizz", "buzz")
        }, new double[]{
            0.1, 0.8, 0.7
        }, "e1", "e2"));
        data.add(new FeatureSetWithScore(0.9, new StringPair[]{
            new StringPair("foo", "bar"),
            new StringPair("this", "that"),
            new StringPair("fizz", "buzz")
        }, new double[]{
            0.8, 0.1, 0.2
        }, "e1", "e2"));
        data.add(new FeatureSetWithScore(0.2, new StringPair[]{
            new StringPair("foo", "bar"),
            new StringPair("this", "that"),
            new StringPair("fizz", "buzz")
        }, new double[]{
            0.2, 0.7, 0.8
        }, "e1", "e2"));
        Scorer trained = trainer.train(data, NaiscListener.DEFAULT);
        FeatureSet fs = new FeatureSet(new StringPair[]{
            new StringPair("foo", "bar"),
            new StringPair("this", "that"),
            new StringPair("fizz", "buzz")
        }, new double[]{
            0.1, 0.8, 0.7
        }, "e1", "e2");
        System.err.println(trained.similarity(data.get(0)));
        System.err.println(trained.similarity(data.get(1)));
        System.err.println(trained.similarity(data.get(2)));
        double score = trained.similarity(fs);
        assert(Double.isFinite(score));
        assert(score < 0.630);
    }
    
    @Test
    public void testGradient() {
        Random r = new Random();
        for(int i = 0; i < 10; i++) {
            double[][] data = new double[100][10];
            double[] scores = new double[100];
            for(int j = 0; j < 100; j++) {
                for(int k = 0; k < 10; k++) {
                    data[j][k] = r.nextDouble();
                }
                scores[j] = r.nextDouble();
            }
            double[] g = new double[12];
            LogitFunction f = new Logit.LogitFunction(data, scores);
            double[] x = new double[12];
            for(int j = 0; j < 12; j++) {
                x[j] = r.nextDouble();
            }
            double f0 = f.evaluate(x, g);
            double[] g2 = new double[12];
            for(int j = 0; j < 12; j++) {
                x[j] += 1e-6;
                double f1 = f.evaluate(x, g2);
                assertEquals(g[j],(f1 - f0) / 1e-6, 0.01);
                x[j] -= 1e-6;
            }
        }
    }

}
