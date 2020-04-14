package org.insightcentre.uld.naisc.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.ExecuteListeners;
import org.insightcentre.uld.naisc.util.Lazy;

import java.io.IOException;
import java.util.HashMap;

public class ConfigurationManager {

    private static final HashMap<String, Configuration> configurations = new HashMap<>();
    private static final HashMap<String, Dataset> datasets = new HashMap<>();

    private static Configuration loadConfiguration(String configuration) throws InvalidConfigurationException {
        if(configurations.containsKey(configuration)) {
            return configurations.get(configuration);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            Configuration config = mapper.readValue("configs/" + configuration + ".json", Configuration.class);
            configurations.put(configuration, config);
            return config;
        } catch(Exception x) {
            throw new InvalidConfigurationException(x.getMessage());
        }
    }

    public static Dataset loadDataset(String dataset) throws DatasetNotFoundException {
        throw new UnsupportedOperationException("TODO");
    }

    public static BlockingStrategy getStrategy(String configuration, String dataset1, String dataset2) throws InvalidConfigurationException, DatasetNotFoundException, IOException {
        Dataset left = loadDataset(dataset1);
        Dataset right = loadDataset(dataset2);
        return loadConfiguration(configuration).makeBlockingStrategy(new Lazy<Analysis>() {
            @Override
            protected Analysis init() {
                DatasetAnalyzer analyzer = new DatasetAnalyzer();
                return analyzer.analyseModel(left, right);
            }
        }, ExecuteListeners.STDERR);

    }
}
