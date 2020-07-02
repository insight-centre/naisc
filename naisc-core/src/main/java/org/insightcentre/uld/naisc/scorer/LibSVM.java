package org.insightcentre.uld.naisc.scorer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import libsvm.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.jena.ext.xerces.impl.dv.util.Base64;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.insightcentre.uld.naisc.Alignment.SKOS_EXACT_MATCH;

/**
 * The LibSVM scorer returns the probability that a particular property holds
 * given its input
 *
 * @author John McCrae
 */
public class LibSVM implements ScorerFactory {

    /**
     * The configuration for LibSVM models
     */
    public static class Configuration {
        /**
         * The property to output.
         */
        @ConfigurationParameter(description = "The property to output")
        public String property = SKOS_EXACT_MATCH;
        /**
         * Print analysis of features.
         */
        @ConfigurationParameter(description = "Print analysis of features")
        public boolean perFeature = false;
        /**
         * Write the training data to an ARFF file. This gives an intermediate
         * version of the data useful in training the model.
         */
        //public String arffFile;
    }

    private static ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public Scorer makeScorer(Map<String, Object> params, File objectFile) {
        Configuration config = mapper.convertValue(params, Configuration.class);
        if (!objectFile.exists()) {
            throw new ConfigurationException("Model file does not exist. (Perhaps you need to train this model?)");
        }
        try {
            return load(objectFile, config);
        } catch (IOException x) {
            throw new ConfigurationException(x);
        }
    }

    @Override
    public Option<ScorerTrainer> makeTrainer(Map<String, Object> params, String property, File objectFile) {
        Configuration config = mapper.convertValue(params, Configuration.class);
        if (property.equals(config.property)) {
            if (objectFile == null) {
                throw new ConfigurationException("Model file is a required parameter of LibSVM");
            }
            return new Some<>(new LibSVMTrainer(config, objectFile));
        } else {
            return new None<>();
        }
    }

    @Override
    public String id() {
        return "libsvm";
    }

    static svm_problem makeInstances(FeatureSet example) {
        //List<String> featNames = buildAttributes(example, _featNames);
        svm_problem prob = new svm_problem();
        prob.l = 0;
        return prob;

    }
//
//    private static ArrayList<String> buildAttributes(FeatureSet example, String[] featNames) {
//        ArrayList<String> attributes = new ArrayList<>();
//        int i = 0;
//        for (StringPair name : example.names) {
//            String featName = name._1 + "-" + name._2;
//            if (featNames != null) {
//                featNames[i++] = featName;
//            }
//            attributes.add(featName);
//        }
//        return attributes;
//    }

    public static void analyzeFeatures(svm_problem instances) {
        final PearsonsCorrelation corr = new PearsonsCorrelation();
        double[] sim = new double[instances.x.length];
        for (int j = 0; j < instances.x.length; j++) {
            sim[j] = instances.y[j];
        }
        final List<String> ignored = new ArrayList<>();
        System.out.println();
        System.out.println("| Feature                                  | Correl    |");
        System.out.println("|:-----------------------------------------|:---------:|");
        for (int i = 0; i < instances.x[0].length - 1; i++) {
            double[] v = new double[instances.x.length];
            for (int j = 0; j < instances.x.length; j++) {
                v[j] = instances.x[j][i].value;
            }
            double c = corr.correlation(sim, v);
            if (Double.isNaN(c)) {
                ignored.add("Instance " + i);
            } else {
                System.out.println(String.format("| % 3d | % .6f |", i, c));
            }
        }
        if (!ignored.isEmpty()) {
            System.out.print("Ignoring: ");
            for (String i : ignored) {
                System.out.print(i + " ");
            }
            System.out.println();
        }
    }

    private svm_parameter makeParameters() {
        svm_parameter param = new svm_parameter();
        param.probability = 1;
        param.gamma = 0.5;
        param.nu = 0.5;
        param.C = 1;
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.cache_size = 20000;
        param.eps = 0.001;
        return param;
    }

    private class LibSVMTrainer implements ScorerTrainer {

        private final boolean perFeature;
        private final String property;
        private final File modelFile;

        public LibSVMTrainer(Configuration configuration, File modelFile) {
            this.perFeature = configuration.perFeature;
            this.property = configuration.property == null ? Alignment.SKOS_EXACT_MATCH : configuration.property;
            this.modelFile = modelFile;
            if (modelFile == null) {
                throw new IllegalArgumentException("Model file cannot be null");
            }
        }

        @Override
        public Scorer train(List<FeatureSetWithScore> dataset, NaiscListener log) {
            if (dataset.isEmpty()) {
                throw new RuntimeException("Cannot train LibSVM on an empty dataset");
            }
            String[] featNames = null;
            final svm_problem prob = makeInstances(dataset.get(0));
            //final boolean perFeature = this.perFeature;

            ArrayList<svm_node[]> xs = new ArrayList<>();
            DoubleList ys = new DoubleArrayList();
            int dim = 0;
            for (FeatureSetWithScore fss : dataset) {
                if (featNames == null) {
                    featNames = extractFeats(fss);
                }
                Instance inst = buildInstance(fss, fss.score, prob);
                xs.add(inst.x);
                ys.add(inst.y > 0.5 ? 1.0 : -1.0);
                dim = fss.values.length;
            }
            System.err.printf("Problem is %d examples of dimension %d\n", xs.size(), dim);
            prob.x = xs.toArray(new svm_node[xs.size()][]);
            prob.y = ys.toDoubleArray();
            prob.l = xs.size();

            if (perFeature) {
                analyzeFeatures(prob);
            }

            svm_parameter param = makeParameters();

            libsvm.svm_model model;
            try {
                model = libsvm.svm.svm_train(prob, param);
            } catch (Exception x) {
                throw new RuntimeException("Could not train classifier", x);
            }
            final LibSVMClassifier classifier = new LibSVMClassifier(model, featNames, property);

            /*try {
                classifier.save(new File(modelFile));
            } catch (IOException x) {
                throw new RuntimeException(x);
            }*/

            return classifier;
        }

        @Override
        public String property() {
            return property;
        }

        private String[] extractFeats(FeatureSetWithScore fss) {
            String[] feats = new String[fss.names.length];
            for (int i = 0; i < fss.names.length; i++) {
                feats[i] = fss.names[i]._1 + "-" + fss.names[i]._2;
                for (int j = 0; j < i; j++) {
                    if (feats[j].equals(feats[i])) {
                        throw new IllegalArgumentException("Features with same name: " + feats[j]);
                    }
                }
            }
            return feats;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void save(Scorer scorer) throws IOException {
            if(scorer instanceof LibSVMClassifier) {
                ((LibSVMClassifier)scorer).save(modelFile);
            } else {
                throw new IllegalArgumentException("SVM trainer can only save SVM models");
            }
        }
        
        
    }

//    static ArrayList<Attribute> buildAttributes(FeatureSet example, String[] featNames) {
//        ArrayList<Attribute> attributes = new ArrayList<>();
//        int i = 0;
//        for (StringPair name : example.names) {
//            String featName = name._1 + "-" + name._2;
//            if (featNames != null) {
//                featNames[i++] = featName;
//            }
//            attributes.add(new Attribute(featName));
//        }
//        attributes.add(new Attribute("probability"));
//        return attributes;
//    }
    /**
     * The LibSVM Model as stored on disk
     */
    public static class LibSVMModel {

        /**
         * The property trained on
         */
        public String relation;
        /**
         * The name of the features used
         */
        public String[] featNames;
        /**
         * The SVM model (Base64 encoded)
         */
        public String data;
    }

    private LibSVMClassifier load(File file, Configuration config) throws IOException {
        LibSVMModel model = mapper.readValue(file, LibSVMModel.class);
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(Base64.decode(model.data))))) {
            final svm_model c = svm.svm_load_model(br);
            return new LibSVMClassifier(c, model.featNames, config.property == null ? model.relation : config.property);
        }
    }

    static class Instance {

        svm_node[] x;
        double y;

        public Instance(svm_node[] x, double y) {
            this.x = x;
            this.y = y;
        }

    }

    private static boolean LIBSVM_NAN_WARNING = false;

    static Instance buildInstance(FeatureSet fss, double score, svm_problem instances) {
        //        final Instance instance = new DenseInstance(N);
//        for(int i = 0; i < fss.values.length; i++) {
//            instance.setValue(i, fss.values[i]);
//        }
//        instance.setValue(instance.numAttributes() - 1, probability);
        final double[] d = new double[fss.values.length + 1];
        System.arraycopy(fss.values, 0, d, 0, fss.values.length);
        d[fss.values.length] = score;

        final svm_node[] instance = new svm_node[fss.names.length];
        for (int i = 0; i < fss.values.length; i++) {
            instance[i] = new svm_node();
            instance[i].index = i;

            if (!Double.isFinite(fss.values[i])) {
                if (!LIBSVM_NAN_WARNING) {
                    System.err.println("Not a number generated... LibSVM does not support this. Setting to zero but you should examine the feature " + fss.names[i]._1 + "-" + fss.names[i]._2);
                    LIBSVM_NAN_WARNING = true;
                }
                instance[i].value = 0.0;
            } else {
                instance[i].value = fss.values[i];
            }
        }

        //instances.add(instance);
        return new Instance(instance, score);
    }

    private static class LibSVMClassifier implements Scorer {

        private final svm_model classifier;
        private final String[] featNames;
        private final String relation;

        /**
         * Create a LibSVM classifier
         *
         * @param classifier The trained classifier
         * @param featNames The names of the features used
         * @param relation The property to output
         */
        public LibSVMClassifier(svm_model classifier, String[] featNames,
                String relation) {
            this.classifier = classifier;
            this.featNames = featNames;
            this.relation = relation;
        }

        boolean checkedFeatures = false;

        @Override
        public List<ScoreResult> similarity(FeatureSet features, NaiscListener log) throws ModelNotTrainedException{
            final svm_problem instances = makeInstances(features);
            final Instance instance = buildInstance(features, 0.0, instances);
            if (features.names.length != featNames.length) {
                throw new ModelNotTrainedException("Classifier has wrong number of attributes. Model does not match trained");
            }
            if (!checkedFeatures) {
                for (int i = 0; i < featNames.length; i++) {
                    if (!featNames[i].equals(features.names[i]._1 + "-" + features.names[i]._2)) {
                        throw new ModelNotTrainedException("Feature names are not equal: " + featNames[i] + " vs " + features.names[i]);
                    }
                }
                checkedFeatures = true;
            }
            assert (instance.x.length == featNames.length);
            //instances.x = new svm_node[][] { instance };
            //instance.setDataset(instances);
            try {
                int totalClasses = 2;
                int[] labels = new int[totalClasses];
                svm.svm_get_labels(classifier, labels);

                double[] prob_estimates = new double[totalClasses];
                svm.svm_predict_probability(classifier, instance.x, prob_estimates);
                double sim = prob_estimates[0];
                return Collections.singletonList(new ScoreResult(sim, relation));
            } catch (IndexOutOfBoundsException x) {
                throw new RuntimeException("Length of test-time vector is not the same as train. Please retrain the model for this configuration", x);
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        }

        public void save(File file) throws IOException {
            LibSVMModel model = new LibSVMModel();
            model.featNames = featNames;
            model.relation = relation;
            File tmpFile = File.createTempFile("libsvm", "model");
            tmpFile.deleteOnExit();
            svm.svm_save_model(tmpFile.getAbsolutePath(), classifier);
            model.data = Base64.encode(IOUtils.toByteArray(new FileInputStream(tmpFile)));
            tmpFile.delete();
            mapper.writeValue(file, model);
        }

        @Override
        public void close() throws IOException {
        }

    }
}
