package org.insightcentre.uld.naisc.scorer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static org.insightcentre.uld.naisc.scorer.LogGap.removeNaNs;

import java.util.*;

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
    public Scorer makeScorer(Map<String, Object> params, File modelFile) {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addKeyDeserializer(StringPair.class, new StdKeyDeserializer(0, StringPair.class) {
            @Override
            public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
                String[] ss = key.split("--");
                if (ss.length != 2) {
                    throw new RuntimeException("String pair not in correct format: " + key);
                }
                return new StringPair(ss[0].replaceAll("\\\\-", "-"), ss[1].replaceAll("\\\\-", "--"));
            }
        }
        );
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(simpleModule);
        //Configuration config = mapper.convertValue(params, Configuration.class);
        final Scorer scorer;
        if (modelFile == null || !modelFile.exists()) {
            scorer = new RAdLRImpl(Collections.singletonList(new RAdLRModel()));
        } else {
            try {
                List<RAdLRModel> models = mapper.readValue(modelFile, mapper.getTypeFactory().constructCollectionLikeType(List.class, RAdLRModel.class));
                scorer = new RAdLRImpl(models);
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        }
        return scorer;
    }

    @Override
    public Option<ScorerTrainer> makeTrainer(Map<String, Object> params, String property, File modelFile) {
        ObjectMapper mapper = new ObjectMapper();
        if (modelFile == null) {
            throw new RuntimeException("Model file cannot be null");
        }
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
            SimpleModule module = new SimpleModule();
            module.addKeySerializer(StringPair.class, new StdSerializer<StringPair>(StringPair.class) {
                @Override
                public void serialize(StringPair t, JsonGenerator jg, SerializerProvider sp) throws IOException {
                    jg.writeFieldName(t._1.replaceAll("-", "\\\\-") + "--" + t._2.replaceAll("-", "\\\\-"));
                }
            });
            mapper.registerModule(module);

        }

        public void save(String property, List<RAdLRModel> models) throws IOException {
            mapper.writeValue(modelFile, models);
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
        public Object2DoubleOpenHashMap<StringPair> weights = new Object2DoubleOpenHashMap<>();
        public String property = Alignment.SKOS_EXACT_MATCH;
        public final Map<StringPair, LogGap.LogGapModel> feats = new HashMap<>();

        @JsonIgnore
        public LogGap getFeatModel(StringPair sp) {
            LogGap.LogGapModel lgm = feats.get(sp);
            if(sp != null) {
                return LogGap.fromModel(lgm);
            } else {
                return null;
            }
        }
    }

    private static class RAdLRImpl implements Scorer {

        private final List<RAdLRModel> models;

        public RAdLRImpl(List<RAdLRModel> models) {
            this.models = models;
        }

        @Override
        public List<ScoreResult> similarity(FeatureSet features, NaiscListener log) {
            List<ScoreResult> results = new ArrayList<>();
            for(RAdLRModel model : models) {
                DoubleList weights = new DoubleArrayList(features.names.length);
                double[] result = new double[features.names.length];
                LogGap[] loggaps = new LogGap[features.names.length];
                //List<ScoreResult> result = new ArrayList<>(features.names.length);
                for (int i = 0; i < features.names.length; i++) {
                    weights.add(model.weights.getOrDefault(features.names[i], 1.0));
                    synchronized (model) {
                        final LogGap lg;
                        if (!model.feats.containsKey(features.names[i])) {
                            lg = new LogGap();
                        } else {
                            lg = LogGap.fromModel(model.feats.get(features.names[i]));
                        }
                        //result.add(feats.get(features.names[i]).result(features.values[i]));
                        lg.addResult(features.values[i]);
                        result[i] = features.values[i];
                        loggaps[i] = lg;
                    }
                }
                results.add(new ScoreResult(logGapScore(result, weights, model, loggaps, model.property), model.property));
            }
            return results;
        }

        @Override
        public void close() throws IOException {
        }

    }


    private static double logGapScore(double[] features, DoubleList weights, RAdLRModel model, LogGap[] feats, String relation) {
        double x = 0.0;
        int n = 0;
        for (int i = 0; i < features.length; i++) {
            if (Double.isFinite(features[i])) {
                x += weights.get(i) * feats[i].normalize(features[i]);
                n++;
            }
        }
        if (n > 0) {
            x /= n;
        }
       return 1.0 / (1.0 + exp(-model.alpha * x - model.beta));
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
                for(int k = 0; k < fss.values.length; k++) {
                    if(!Double.isFinite(fss.values[k])) {
                        throw new IllegalArgumentException("Non-finite score for " + fss.names[k]);
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
            LogGap[] logGaps = new LogGap[data[0].length];
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
                    logGaps[j] = lg;
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
            normalizeSoln(soln);
            model.alpha = soln[0];
            model.beta = soln[1];
            for (Object2IntMap.Entry<StringPair> e : featureIDs.object2IntEntrySet()) {
                model.weights.put(e.getKey(), soln[e.getIntValue() + 2]);
                model.feats.put(e.getKey(), logGaps[e.getIntValue()].toModel(100));
            }

            return new RAdLRImpl(Collections.singletonList(model));
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
                serializer.save(model.property, ((RAdLRImpl) scorer).models);
            } else {
                throw new IllegalArgumentException("RAdLR cannot serialize models not created by RAdLR");
            }
        }

        private void normalizeSoln(double[] soln) {
            double sumSq = 0.0;
            for (int i = 2; i < soln.length; i++) {
                sumSq += soln[i] * soln[i];
            }
            if (sumSq == 0.0 || soln.length == 2) {
                return;
            }
            sumSq /= soln.length - 2;
            sumSq = Math.sqrt(sumSq);
            soln[0] *= sumSq;
            for (int i = 2; i < soln.length; i++) {
                soln[i] /= sumSq;
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
                Random r = new Random();
                for(int i = 0; i < init.length; i++) {
                    init[i] = r.nextDouble() * 2.0 - 1;
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
            if(data.length == 0) {
                throw new IllegalArgumentException("Data is empty");
            }
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
            if(data.length == 0) {
                throw new IllegalArgumentException("Data is empty");
            }
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
                if(P + Q != 0.0) {
                    g[i] = -1.0 / (P + Q) / (P + Q) * ((P + Q) * qd[i] - PQ * d[i]);
                }
            }
            if(P + Q != 0.0) {
                return -PQ / (P + Q);
            } else {
                return 0.0;
            }
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
                if(Double.isFinite(grad[j])) {
                    double gjj = grad[j] * grad[j];
                    gradSum += gjj;
                    gsum[j] += gjj;
                }
            }
            if (i % 1000 == 0) {
                log.message(Stage.TRAINING, Level.INFO, String.format("Iteration %d: Value=%.8f, Gradient=%.8f", i, fx, Math.sqrt(gradSum)));
            }
            if (Math.sqrt(gradSum) < tolerance) {
                return x;
            }
            // Learn
            for (int j = 0; j < n; j++) {
                if(Double.isFinite(grad[j])) {
                    x[j] -= initialLearningRate / Math.sqrt(gsum[j] + smoothing) * grad[j];
                }
            }
        }
        return x;
    }

}
