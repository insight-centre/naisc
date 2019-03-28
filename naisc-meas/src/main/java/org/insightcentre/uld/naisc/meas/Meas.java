package org.insightcentre.uld.naisc.meas;

import org.insightcentre.uld.naisc.meas.execution.Execution;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.insightcentre.uld.naisc.Alignment.Valid;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.ExecuteListener.Stage;
import org.insightcentre.uld.naisc.meas.ExecuteServlet.Dataset;
import org.insightcentre.uld.naisc.util.LangStringPair;

/**
 * The main work of the Meas applet
 *
 * @author John McCrae
 */
public class Meas {

    static ObjectMapper mapper = new ObjectMapper();
    public static Data data = new Data();

    static {
        data.runs = runs();
        data.configs = configNames();
        data.datasetNames = datasetNames();
    }

    public static String json() {
        try {
            data.activeRuns = ExecuteServlet.activeRuns();
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException x) {
            throw new RuntimeException(x);
        }
    }

    private static Map<String, Configuration> configNames() {
        try {
            File f = new File("configs/");
            if (f.exists() && f.isDirectory()) {
                Map<String, Configuration> configs = new HashMap<>();
                for (File f2 : f.listFiles()) {
                    if (f2.getPath().endsWith(".json")) {
                        try {
                            configs.put(f2.getName().substring(0, f2.getName().length() - 5),
                                    mapper.readValue(f2, Configuration.class));
                        } catch (Exception x) {
                            System.err.printf("Failed to load %s due to %s (%s)\n", f2.getName(),
                                    x.getClass().getName(), x.getMessage());
                        }
                    }
                }
                return configs;
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
        return Collections.EMPTY_MAP;
    }

    private static List<String> datasetNames() {
        try {
            File f = new File("datasets/");
            if (f.exists() && f.isDirectory()) {
                List<String> datasets = new ArrayList<>();
                for (File f2 : f.listFiles()) {
                    if (Dataset.isDataset(f2)) {
                        datasets.add(f2.getName());
                    }
                }
                return datasets;
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    private static List<Run> runs() {
        try {
            File f = new File("runs");
            if (f.exists() && f.isDirectory()) {
                List<Run> runs = new ArrayList<>();
                RUNS:
                for (File f2 : f.listFiles()) {
                    if (f2.getName().endsWith(".db")) {
                        String runName = f2.getName().substring(0, f2.getName().length() - 3);
                        for (ActiveRun r : data.activeRuns) {
                            if (r.identifier.equals(runName)) {
                                continue RUNS;
                            }
                        }
                        try {
                            Run run = Execution.loadRun(runName);
                            if (run != null) {
                                runs.add(run);
                            }
                        } catch (Exception x) {
                            x.printStackTrace();
                        }
                    }
                }
                return Collections.synchronizedList(runs);
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
        return Collections.synchronizedList(new ArrayList<>());
    }

    public static void updateRun(String id, Run newRun) {
        ListIterator<Run> iter = data.runs.listIterator();
        while (iter.hasNext()) {
            if (iter.next().identifier.equals(id)) {
                iter.set(newRun);
                return;
            }
        }
    }

    public static class Data {

        public List<ActiveRun> activeRuns = Collections.EMPTY_LIST;
        public List<Run> runs;
        public Map<String, Configuration> configs;
        public List<String> datasetNames;
        public Configuration config = null;
        public String configName;
        public String datasetName;
        public String identifier;
        public boolean showConfig = false;
    }

    public static class Run {

        public String identifier;
        public String configName;
        public String datasetName;
        public double precision, recall, fmeasure, correlation;
        public long time;
        public boolean isTrain;

        public Run() {
        }

        public Run(String identifier, String configName, String datasetName, double precision, double recall, double fmeasure, double correlation, long time, boolean isTrain) {
            this.identifier = identifier;
            this.configName = configName;
            this.datasetName = datasetName;
            this.precision = precision;
            this.recall = recall;
            this.fmeasure = fmeasure;
            this.correlation = correlation;
            this.time = time;
            this.isTrain = isTrain;
        }

    }

    public static class ActiveRun {

        public String identifier;
        public String configName;
        public String datasetName;
        public Stage stage;
        public String status;
        public boolean active;

        public ActiveRun(String identifier, String configName, String datasetName, Stage stage, String status, boolean active) {
            this.identifier = identifier;
            this.configName = configName;
            this.datasetName = datasetName;
            this.stage = stage;
            this.status = status;
            this.active = active;
        }

        public ActiveRun() {
        }
        
        
    }

    public static String loadRunResult(String id, int offset, int limit) throws JsonProcessingException, IOException {
        if (!id.matches(ExecuteServlet.VALID_ID)) {
            throw new IllegalArgumentException("Bad ID");
        }
        List<RunResultRow> rows = Execution.loadData(id, offset, limit);
        return mapper.writeValueAsString(rows);
    }

    public static class RunResultRow {

        public String subject;
        public String property;
        public String object;
        public Map<String, LangStringPair> lens;
        public double score;
        public Valid valid;
        public int idx;
    }

}
