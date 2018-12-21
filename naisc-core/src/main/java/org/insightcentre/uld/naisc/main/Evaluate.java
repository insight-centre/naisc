package org.insightcentre.uld.naisc.main;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.Alignment.Valid;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Tools for evaluating an alignment relative to a gold standard evaluation
 *
 * @author John McCrae
 */
public class Evaluate {

    public static class EvaluationResults {
        public int tp;
        public int fp;
        public int fn;
        public double correlation;
        public List<Pair<Double,EvaluationResults>> thresholds = new ArrayList<>();
        
        public double precision() {
            return (double)tp / (double)(tp + fp);
        }
        
        public double recall() {
            return (double)tp / (double)(tp + fn);
        }
        
        public double fmeasure() {
            return 2.0 * tp / (double)(2.0 * tp + fp + fn);
        }
    }

    public static EvaluationResults evaluate(AlignmentSet output, Map<Property, Object2DoubleMap<Statement>> gold) {
        PearsonsCorrelation correlation = new PearsonsCorrelation();
        EvaluationResults er = new EvaluationResults();
        for (int i = 0; i <= 10; i++) {
            er.thresholds.add(new Pair(0.1 * i,new EvaluationResults()));
        }
        Set<Statement> seen = new HashSet<>();
        DoubleList outputScores = new DoubleArrayList(), goldScores = new DoubleArrayList();
        Model m = ModelFactory.createDefaultModel();
        for (Alignment align : output) {
            if (align.valid != Valid.unknown) {
                continue;
            }
            Property p = m.createProperty(align.relation);
            if (gold.containsKey(p)) {
                Statement st = m.createStatement(m.createResource(align.entity1), p, m.createResource(align.entity2));
                if (gold.get(p).containsKey(st)) {
                    double gScore = gold.get(p).getDouble(st);
                    outputScores.add(align.score);
                    goldScores.add(gScore);
                    er.tp++;
                    for(int i = 0; 0.1 * i <= align.score; i++) {
                        er.thresholds.get(i)._2.tp++;
                    }
                    align.valid = Valid.yes;
                    seen.add(st);
                } else {
                    er.fp++;
                    for (int i = 0; 0.1 * i <= align.score; i++) {
                        er.thresholds.get(i)._2.fp++;
                    }
                    align.valid = Valid.no;
                }
            } else {
                er.fp++;
                for (int i = 0; 0.1 *i <= align.score; i++) {
                    er.thresholds.get(i)._2.fp++;
                }
                align.valid = Valid.no;
            }
        }
        int goldSize = 0;
        for (Map.Entry<Property, Object2DoubleMap<Statement>> e : gold.entrySet()) {
            goldSize += e.getValue().size();
            try {
                for (Object2DoubleMap.Entry<Statement> e2 : e.getValue().object2DoubleEntrySet()) {
                    if (!seen.contains(e2.getKey())) {
                        output.add(new Alignment(e2.getKey(), e2.getDoubleValue(), Valid.novel));
                    }
                }
            } catch (UnsupportedOperationException x) {
                // The alignments cannot be expanded 
            }
        }
        er.fn = goldSize - er.tp;
        for (int i = 0; i <= 10; i++) {
            er.thresholds.get(i)._2.fn = goldSize - er.thresholds.get(i)._2.tp;
        }
        if(goldScores.size() >= 2)
            er.correlation = correlation.correlation(goldScores.toDoubleArray(), outputScores.toDoubleArray());
        else
            er.correlation = 0;
        return er;
    }

    public static void main(String[] args) {

    }

}
