package org.insightcentre.uld.naisc.scorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.FeatureSetWithScore;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.NaiscListener.Level;
import org.insightcentre.uld.naisc.NaiscListener.Stage;
import org.insightcentre.uld.naisc.ScoreResult;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.ScorerFactory;
import org.insightcentre.uld.naisc.ScorerTrainer;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;
import org.insightcentre.uld.naisc.util.StringPair;

/**
 * Robust adaptive logistic regression. This classifier aims to provide a robust
 * implementation over missing features, features not in training and feature
 * distributions
 *
 * @author John McCrae
 */
public class RAdLR implements ScorerFactory {

    @Override
    public String id() {
        return "radlr";
    }

    @Override
    public List<Scorer> makeScorer(Map<String, Object> params, File modelFile) {
        ObjectMapper mapper = new ObjectMapper();
        //Configuration config = mapper.convertValue(params, Configuration.class);
        final List<Scorer> scorers = new ArrayList<>();
        if (modelFile == null || !modelFile.exists()) {
            scorers.add(new RAdLRImpl(new RAdLRModel()));
        } else {
            try {
                List<RAdLRModel> models = mapper.readValue(modelFile, mapper.getTypeFactory().constructCollectionLikeType(List.class, RAdLRModel.class));
                for (RAdLRModel model : models) {
                    scorers.add(new RAdLRImpl(model));
                }
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        }
        return scorers;
    }

    @Override
    public Option<ScorerTrainer> makeTrainer(Map<String, Object> params, String property, File modelFile) {
        ObjectMapper mapper = new ObjectMapper();
        Configuration config = mapper.convertValue(params, Configuration.class);
        RAdLRModel model = new RAdLRModel();
        model.property = property;
        TrainSerializer serializer = new TrainSerializer(modelFile, mapper);
        return new Some<>(new RAdLRTrainer(model, serializer, config.errorFunction));
    }

    private static class TrainSerializer {

        private final Map<String, RAdLRModel> models = new HashMap<>();
        private final File modelFile;
        private final ObjectMapper mapper;

        public TrainSerializer(File modelFile, ObjectMapper mapper) {
            this.modelFile = modelFile;
            this.mapper = mapper;
        }

        public void save(String property, RAdLRModel model) throws IOException {
            models.put(property, model);
            List<RAdLRModel> modelList = new ArrayList<>(models.values());
            mapper.writeValue(modelFile, modelList);
        }
    }

    public static enum ErrorFunction {
        KullbackLeibler,
        SoftkullbackLeibler,
        FMeasure
    }

    public static class Configuration {

        @ConfigurationParameter(description = "The error function to use in training")
        public ErrorFunction errorFunction = ErrorFunction.FMeasure;

    }

    public static class RAdLRModel {

        public double alpha = 1.0, beta = 0.0;
        public Object2DoubleMap<StringPair> weights = new Object2DoubleOpenHashMap<>();
        public String property = Alignment.SKOS_EXACT_MATCH;
    }

    private static class RAdLRImpl implements Scorer {

        private final RAdLRModel model;
        private final Map<StringPair, LogGap> feats = new HashMap<>();

        public RAdLRImpl(RAdLRModel model) {
            this.model = model;
        }

        @Override
        public ScoreResult similarity(FeatureSet features, NaiscListener log) {
            DoubleList weights = new DoubleArrayList(features.names.length);
            List<ScoreResult> result = new ArrayList<>(features.names.length);
            for (int i = 0; i < features.names.length; i++) {
                weights.add(model.weights.getOrDefault(features.names[i], 1.0));
                if (!feats.containsKey(features.names[i])) {
                    feats.put(features.names[i], new LogGap());
                }
                result.add(feats.get(features.names[i]).result(features.values[i]));
            }
            return new LogGapScorer(result, weights, model);
        }

        @Override
        public String relation() {
            return model.property;
        }

        @Override
        public void close() throws IOException {
        }

    }

    private static class LogGapScorer implements ScoreResult {

        private final List<ScoreResult> features;
        private final DoubleList weights;
        private final RAdLRModel model;

        public LogGapScorer(List<ScoreResult> features, DoubleList weights, RAdLRModel model) {
            this.features = features;
            this.weights = weights;
            this.model = model;
        }

        @Override
        public double value() {
            double x = 0.0;
            int n = 0;
            for (int i = 0; i < features.size(); i++) {
                if (Double.isFinite(features.get(i).value())) {
                    x += weights.get(i) * features.get(i).value();
                    n++;
                }
            }
            x /= n;
            return 1.0 / (1.0 + exp(-model.alpha * x - model.beta));
        }

    }

    private static class RAdLRTrainer implements ScorerTrainer {

        private final RAdLRModel model;
        private final TrainSerializer serializer;
        private final ErrorFunction errorFunction;

        public RAdLRTrainer(RAdLRModel model, TrainSerializer serializer, ErrorFunction errorFunction) {
            this.model = model;
            this.serializer = serializer;
            this.errorFunction = errorFunction;
        }

        @Override
        public Scorer train(List<FeatureSetWithScore> dataset, NaiscListener log) {
            double[][] data = new double[dataset.size()][];
            double[] scores = new double[dataset.size()];
            Object2IntMap<StringPair> featureIDs = new Object2IntOpenHashMap<>();
            int i = 0;
            for (FeatureSetWithScore fss : dataset) {
                for (StringPair fid : fss.names) {
                    if (!featureIDs.containsKey(fid)) {
                        featureIDs.put(fid, featureIDs.size());
                    }
                }
                scores[i++] = fss.score;
            }
            i = 0;
            for (FeatureSetWithScore fss : dataset) {
                data[i] = new double[featureIDs.size()];
                Arrays.fill(data[i], Double.NaN);
                for (int j = 0; j < fss.names.length; j++) {
                    data[i][featureIDs.getInt(fss.names[j])] = fss.values[j];
                }
                i++;
            }
            // Apply Log Gap normalization
            if (data.length > 0) {
                for (int j = 0; j < data[0].length; j++) {
                    double[] d = new double[data.length];
                    for (i = 0; i < data.length; i++) {
                        d[i] = data[i][j];
                    }
                    LogGap lg = new LogGap();
                    lg.makeModel(d);
                    for (i = 0; i < data.length; i++) {
                        data[i][j] = lg.normalize(data[i][j]);
                    }
                }
            }
            final Function f;
            switch (errorFunction) {
                case KullbackLeibler:
                    f = new RAdLRFunction(data, scores, 0.0);
                    break;
                case SoftkullbackLeibler:
                    f = new RAdLRFunction(data, scores, Math.exp(-1));
                    break;
                case FMeasure:
                    f = new FMRAdLRFunction(data, scores);
                    break;
                default:
                    throw new RuntimeException("Unexpected or null error function");
            }
            double[] soln = adaGrad(f, log);
            model.alpha = soln[0];
            model.beta = soln[1];
            for (Object2IntMap.Entry<StringPair> e : featureIDs.object2IntEntrySet()) {
                model.weights.put(e.getKey(), soln[e.getIntValue() + 2]);
            }

            return new RAdLRImpl(model);
        }

        @Override
        public String property() {
            return model.property;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void save(Scorer scorer) throws IOException {
            if (scorer instanceof RAdLRImpl) {
                serializer.save(model.property, ((RAdLRImpl) scorer).model);
            } else {
                throw new IllegalArgumentException("RAdLR cannot serialize models not created by RAdLR");
            }
        }

    }

    static abstract class Function {

        final double[][] data;
        final double[] scores;

        protected Function(double[][] data, double[] scores) {
            this.data = data;
            this.scores = scores;
        }

        public abstract double evaluate(double[] x, double[] g);

        public double[] initial() {
            if (data.length > 0) {
                double[] init = new double[data[0].length + 2];
                /*Arrays.fill(init, 1.0);
                init[1] = 0.0;
                return init;*/
                int pos = 0;
                int neg = 0;
                for (double score : scores) {
                    if (score > 0.5) {
                        pos++;
                    } else {
                        neg++;
                    }
                }
                init[0] = 3.0;
                init[1] = -6.0 * neg / (neg + pos);
                PearsonsCorrelation correl = new PearsonsCorrelation();
                double sumsq = 0.0;
                for (int i = 0; i < data[0].length; i++) {
                    double[] z = new double[data.length];
                    for (int j = 0; j < data.length; j++) {
                        z[j] = data[j][i];
                    }
                    init[i + 2] = correl.correlation(z, scores);
                    if (!Double.isFinite(init[i + 2])) {
                        init[i + 2] = 0.0;
                    }
                    sumsq += init[i + 2] * init[i + 2];
                }
                sumsq /= data[0].length;
                for (int i = 0; i < data[0].length; i++) {
                    init[i + 2] /= sumsq;
                }
                return init;

            } else {
                throw new RuntimeException("Cannot learn from empty data");
            }
        }
    }

    static class RAdLRFunction extends Function {

        private final double smooth;

        public RAdLRFunction(double[][] data, double[] scores, double smooth) {
            super(data, scores);
            this.smooth = smooth;
        }

        @Override
        public double evaluate(double[] x, double[] g) {
            Arrays.fill(g, 0.0);
            double alpha = x[0];
            double beta = x[1];
            double value = 0.0;
            for (int i = 0; i < data.length; i++) {
                double z = 0;
                int n = 0;
                for (int j = 0; j < data[i].length; j++) {
                    if (Double.isFinite(data[i][j])) {
                        z += x[j + 2] * data[i][j];
                        n++;
                    }
                }
                if (n == 0) {
                    continue;
                }
                z /= n;
                double q = 1.0 / (1.0 + exp(-alpha * z - beta));
                assert (q != 0.0);
                assert (q != 1.0);
                if (x[0] == 3.0) {
                    System.err.printf("%.4f (%.4f) <-> %.4f = %.4f\n", q, z, scores[i], (scores[i] - 1) * log(1 - q + smooth) - scores[i] * log(q + smooth));
                }
                value += (scores[i] - 1) * log(1 - q + smooth) - scores[i] * log(q + smooth);
                double d = (1 - scores[i]) / (1 - q + smooth) - scores[i] / (q + smooth);
                g[0] += d * q * (1 - q) * z / data.length;
                g[1] += d * q * (1 - q) / data.length;
                for (int j = 0; j < data[i].length; j++) {
                    if (Double.isFinite(data[i][j])) {
                        g[j + 2] += d * q * (1 - q) * alpha * data[i][j] / n / data.length;
                    }
                }
            }
            return value / data.length;
        }
    }

    static class FMRAdLRFunction extends Function {

        public FMRAdLRFunction(double[][] data, double[] scores) {
            super(data, scores);
        }

        @Override
        public double evaluate(double[] x, double[] g) {
            Arrays.fill(g, 0.0);
            double alpha = x[0];
            double beta = x[1];
            double[] qd = new double[g.length];
            double[] d = new double[g.length];
            double PQ = 0.0;
            double P = 0.0;
            double Q = 0.0;
            for (int i = 0; i < data.length; i++) {
                double z = 0;
                int n = 0;
                for (int j = 0; j < data[i].length; j++) {
                    if (Double.isFinite(data[i][j])) {
                        z += x[j + 2] * data[i][j];
                        n++;
                    }
                }
                if (n == 0) {
                    continue;
                }
                z /= n;
                double p = 1.0 / (1.0 + exp(-alpha * z - beta));
                assert (p != 0.0);
                assert (p != 1.0);
                P += p;
                Q += scores[i];
                PQ += scores[i] * p;
                qd[0] += scores[i] * p * (1 - p) * z;
                qd[1] += scores[i] * p * (1 - p);
                d[0] += p * (1 - p) * z;
                d[1] += p * (1 - p);
                for (int j = 0; j < data[i].length; j++) {
                    if (Double.isFinite(data[i][j])) {
                        qd[j + 2] += scores[i] * p * (1 - p) * alpha * data[i][j] / n;
                        d[j + 2] += p * (1 - p) * alpha * data[i][j] / n;
                    }
                }
            }
            for (int i = 0; i < g.length; i++) {
                g[i] = -1.0 / (P + Q) / (P + Q) * ((P + Q) * qd[i] - PQ * d[i]);
            }
            return -PQ / (P + Q);
        }
    }

    private static double[] adaGrad(Function objective, NaiscListener log) {
        return adaGrad(objective, 10000, 0.1, 1e-6, 1e-8, log);
    }

    private static double[] adaGrad(Function objective, int maxIters,
            double initialLearningRate, double tolerance, double smoothing, NaiscListener log) {
        double[] x = objective.initial();
        final int n = x.length;
        double[] grad = new double[x.length];
        double[] gsum = new double[x.length];

        for (int i = 0; i < maxIters; i++) {
            // Grad = d f(x)
            double fx = objective.evaluate(x, grad);
            double gradSum = 0.0;
            // Update gsum
            for (int j = 0; j < n; j++) {
                double gjj = grad[j] * grad[j];
                gradSum += gjj;
                gsum[j] += gjj;
            }
            if (i % 1000 == 0) {
                log.message(Stage.TRAINING, Level.INFO, String.format("Iteration %d: Value=%.8f, Gradient=%.8f", i, fx, Math.sqrt(gradSum)));
            }
            if (Math.sqrt(gradSum) < tolerance) {
                return x;
            }
            // Learn
            for (int j = 0; j < n; j++) {
                x[j] -= initialLearningRate / Math.sqrt(gsum[j] + smoothing) * grad[j];
            }

            // This bit is specific, we want to keep the mean of the weights as zero
            double sum = 0.0;
            for (int j = 2; j < n; j++) {
                sum += x[j];
            }
            sum /= n - 2;
            for (int j = 2; j < n; j++) {
                x[j] /= sum;
            }
        }
        return x;
    }

}
