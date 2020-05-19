package org.insightcentre.uld.naisc.meas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.insightcentre.uld.naisc.Alignment.Valid;
import org.insightcentre.uld.naisc.EvaluationSet;
import org.insightcentre.uld.naisc.NaiscListener.Stage;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.meas.execution.Execution;
import org.insightcentre.uld.naisc.util.LangStringPair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

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
            data.configs = configNames();
            data.datasetNames = datasetNames();
            data.activeRuns = ExecuteServlet.activeRuns();
            data.availableDatasets = getAvailableDataset();
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException x) {
            throw new RuntimeException(x);
        }
    }

    public static String runsJson() {
        try {
            return mapper.writeValueAsString(data.runs);
        } catch(JsonProcessingException x) {
            throw new RuntimeException(x);
        }
    }

    private static Map<String, Configuration> configNames() {
        try {
            File f = new File("configs/");
            if (f.exists() && f.isDirectory()) {
                Map<String, Configuration> configs = new TreeMap<>();
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
                    if (EvaluationSet.isDataset(f2)) {
                        datasets.add(f2.getName());
                    }
                }
                Collections.sort(datasets);
                return datasets;
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
        return new ArrayList<>();
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

    private static List<String> getAvailableDataset() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                new URL("http://server1.nlp.insight-centre.org/naisc-datasets/").
                        openConnection().getInputStream()))) {
            List<String> datasets = new ArrayList<>();
            String line = in.readLine();
            while (line != null) {
                if (line.contains("[DIR]")) {
                    int i1 = line.indexOf("href=\"") + 6;
                    int i2 = line.indexOf("\"", i1) - 1;
                    datasets.add(line.substring(i1, i2));
                }
                line = in.readLine();
            }
            return datasets;
        } catch (IOException x) {
            x.printStackTrace();
        }
        return Collections.EMPTY_LIST;
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
        public List<String> availableDatasets;
        public List<String> messages = new ArrayList<>();
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
        if (id == null) {
            return "[]";
        }
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
        public String leftRoot;
        public String rightRoot;
        public List<String> leftPath;
        public List<String> rightPath;
    }

    public static String loadComparison(String first, String second) throws JsonProcessingException, IOException {

        if (!first.matches(ExecuteServlet.VALID_ID) || !second.matches(ExecuteServlet.VALID_ID)) {
            throw new IllegalArgumentException("Bad ID");
        }
        try {
            List<CompareResultRow> rows = Execution.loadCompare(first, second);
            return mapper.writeValueAsString(rows);
        } catch (Exception x) {
            x.printStackTrace();
            return "[]";
        }
    }

    public static class CompareResultRow {
        public String subject;
        public String property;
        public String object;
        public Map<String, LangStringPair> lens;
        public double firstScore, secondScore;
        public Valid firstValid, secondValid;

        public CompareResultRow() {
        }

        public CompareResultRow(String subject, String property, String object, Map<String, LangStringPair> lens, double firstScore, double secondScore, Valid firstValid, Valid secondValid) {
            this.subject = subject;
            this.property = property;
            this.object = object;
            this.lens = lens;
            this.firstScore = firstScore;
            this.secondScore = secondScore;
            this.firstValid = firstValid;
            this.secondValid = secondValid;
        }
    }
}
