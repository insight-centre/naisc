package org.insightcentre.uld.naisc.scorer;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.FeatureSetWithScore;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.NaiscListener.Level;
import org.insightcentre.uld.naisc.NaiscListener.Stage;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.ScorerFactory;
import org.insightcentre.uld.naisc.ScorerTrainer;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;
import org.insightcentre.uld.naisc.util.StringPair;

/**
 *
 * @author John McCrae
 */
public class Logit implements ScorerFactory {

    @Override
    public String id() {
        return "logit";
    }

    @Override
    public List<Scorer> makeScorer(Map<String, Object> params, File modelFile) {
        ObjectMapper mapper = new ObjectMapper();
        Configuration config = mapper.convertValue(params, Configuration.class);
        final List<Scorer> scorers = new ArrayList<>();
        if (modelFile == null || !modelFile.exists()) {
            scorers.add(new LogitImpl(new LogitModel()));
        } else {
            try {
                List<LogitModel> models = mapper.readValue(modelFile, mapper.getTypeFactory().constructCollectionLikeType(List.class, LogitModel.class));
                for (LogitModel model : models) {
                    scorers.add(new LogitImpl(model));
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
        LogitModel model = new LogitModel();
        model.property = property;
        TrainSerializer serializer = new TrainSerializer(modelFile, mapper);
        return new Some<>(new LogitTrainer(model, serializer));
    }

    private static class TrainSerializer {
        private final Map<String, LogitModel> models = new HashMap<>();
        private final File modelFile;
        private final ObjectMapper mapper;

        public TrainSerializer(File modelFile, ObjectMapper mapper) {
            this.modelFile = modelFile;
            this.mapper = mapper;
        }
        
        
        public void save(String property, LogitModel model) throws IOException {
            models.put(property,model);
            List<LogitModel> modelList = new ArrayList<>(models.values());
            mapper.writeValue(modelFile, modelList);
        }
    }
    
    public static class Configuration {
    }

    public static class LogitModel {

        public double alpha = 1.0, beta = 0.0;
        public Object2DoubleMap<StringPair> weights = new Object2DoubleOpenHashMap<>();
        public String property = Alignment.SKOS_EXACT_MATCH;
    }

    private static class LogitImpl implements Scorer {

        private final LogitModel model;

        public LogitImpl(LogitModel model) {
            this.model = model;
        }

        @Override
        public double similarity(FeatureSet features, NaiscListener log) {
            double x = 0.0;
            for (int i = 0; i < features.names.length; i++) {
                x += model.weights.getOrDefault(features.names[i], 1.0) * features.values[i];
            }
            x /= features.names.length;
            return 1.0 / (1.0 + exp(-model.alpha * x - model.beta));
        }

        @Override
        public String relation() {
            return model.property;
        }

        @Override
        public void close() throws IOException {
        }

    }

    private static class LogitTrainer implements ScorerTrainer {

        private final LogitModel model;
        private final TrainSerializer serializer;

        public LogitTrainer(LogitModel model, TrainSerializer serializer) {
            this.model = model;
            this.serializer = serializer;
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
            LogitFunction f = new LogitFunction(data, scores);
            double[] soln = adaGrad(f, log);
            model.alpha = soln[0];
            model.beta = soln[1];
            for (Object2IntMap.Entry<StringPair> e : featureIDs.object2IntEntrySet()) {
                model.weights.put(e.getKey(), soln[e.getIntValue() + 2]);
            }
            
            return new LogitImpl(model);
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
            if(scorer instanceof LogitImpl) {
                serializer.save(model.property, ((LogitImpl)scorer).model);
            } else {
                throw new IllegalArgumentException("Logit cannot serialize models not created by Logit");
            }
        }

    }

    static class LogitFunction  {

        final double[][] data;
        final double[] scores;

        public LogitFunction(double[][] data, double[] scores) {
            this.data = data;
            this.scores = scores;
        }

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
                z /= n;
                double q = 1.0 / (1.0 + exp(-alpha * z - beta));
                value +=  (scores[i] - 1) * log(1 - q) - scores[i] * log(q);
                double d = (1 - scores[i]) / (1 - q) - scores[i] / q;
                g[0] += d * q * (1 - q) * z;
                g[1] += d * q * (1 - q);
                for (int j = 0; j < data[i].length; j++) {
                    if (Double.isFinite(data[i][j])) {
                        g[j + 2] += d * q * (1 - q) * alpha * data[i][j] / n;
                    }
                }
            }
            return value;
        }

        public double[] initial() {
            if (data.length > 0) {
                double[] init = new double[data[0].length + 2];
                Arrays.fill(init, 1.0);
                init[1] = 0.0;
                return init;
            } else {
                throw new RuntimeException("Cannot learn from empty data");
            }
        }

    }

    private static double[] adaGrad(LogitFunction objective, NaiscListener log) {
        return adaGrad(objective, 1000, 0.1, 1e-6, 1e-8, log);
    }

    private static double[] adaGrad(LogitFunction objective, int maxIters,
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
            log.message(Stage.TRAINING, Level.INFO, String.format("Iteration %d: Value=%.8f, Gradient=%.8f", i, fx, Math.sqrt(gradSum)));
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
