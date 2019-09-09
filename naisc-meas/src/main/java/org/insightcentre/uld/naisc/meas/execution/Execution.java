package org.insightcentre.uld.naisc.meas.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import eu.monnetproject.lang.Language;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment.Valid;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.main.ExecuteListener;
import org.insightcentre.uld.naisc.meas.DataView;
import org.insightcentre.uld.naisc.meas.DataView.DataViewEntry;
import org.insightcentre.uld.naisc.meas.DataView.DataViewPath;
import org.insightcentre.uld.naisc.meas.ExecuteServlet;
import org.insightcentre.uld.naisc.meas.Meas.Run;
import org.insightcentre.uld.naisc.meas.Meas.RunResultRow;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * Manages the execution and how it is saved to the database
 *
 * @author John McCrae
 */
public class Execution implements ExecuteListener {

    public ListenerResponse response = new ListenerResponse();
    public boolean aborted = false;
    private final String id;
    private final HashMap<Pair<Resource, Resource>, Map<String, LangStringPair>> lensResults = new HashMap<>();
    private final HashMap<Resource, Map<String, LangStringPair>> leftLensResults = new HashMap<>();
    private final HashMap<Resource, Map<String, LangStringPair>> rightLensResults = new HashMap<>();
    private List<Pair<Resource, Resource>> blocks = new ArrayList<>();
    private final static Object databaseLock = new Object();
    private final List<Message> messages = new ArrayList<>();

    public Execution(String id) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.id = id;
            assert (id == null || id.matches(ExecuteServlet.VALID_ID));
        } catch (ClassNotFoundException x) {
            throw new RuntimeException("SQLite JDBC not available", x);
        }
    }

    @Override
    public void updateStatus(ExecuteListener.Stage stage, String message) {
        if (aborted) {
            throw new RuntimeException("Stopped by user");
        }
        this.response.stage = stage;
        this.response.lastMessage = message;
        System.err.println("[" + stage + "] " + message);
    }

    @Override
    public void addLensResult(Resource id1, Resource id2, String lensId, LangStringPair res) {
        Pair<Resource, Resource> p = new Pair(id1, id2);
        synchronized (lensResults) {
            if (!lensResults.containsKey(p)) {
                lensResults.put(p, new HashMap<>());
            }
            lensResults.get(p).put(lensId, res);
            if (!leftLensResults.containsKey(id1)) {
                leftLensResults.put(id1, new HashMap<>());
            }
            leftLensResults.get(id1).put(lensId, res);
            if (!rightLensResults.containsKey(id1)) {
                rightLensResults.put(id1, new HashMap<>());
            }
            rightLensResults.get(id1).put(lensId, res);
        }
    }

    private static Connection connection(String id) throws SQLException {
        if (id != null) {
            return DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db");
        } else {
            return DriverManager.getConnection("jdbc:sqlite::memory:");
        }
    }

    @Override
    public void addBlock(Resource res1, Resource res2) {
        blocks.add(new Pair<>(res1, res2));
        if (blocks.size() > BLOCK_MAX) {
            synchronized (databaseLock) {
                if (blocks.size() > BLOCK_MAX) {
                    List<Pair<Resource, Resource>> b2 = blocks;
                    blocks = new ArrayList<>();
                    try (Connection connection = connection(id)) {
                        createTables(connection);
                        saveBlocks(connection, b2);
                    } catch (SQLException x) {
                        x.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void message(Stage stage, Level level, String message) {
        this.updateStatus(stage, message);
        messages.add(new Message(stage, level, message));
    }

    private static final int BLOCK_MAX = 100000;

    private boolean tablesCreated = false;

    private void createTables(Connection connection) throws SQLException {
        if (!tablesCreated || id == null) {
            try (Statement stat = connection.createStatement()) {
                stat.execute("CREATE TABLE runs ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "identifier TEXT,\n"
                        + "configName TEXT,\n"
                        + "datasetName TEXT,\n"
                        + "precision REAL, "
                        + "recall REAL,"
                        + "fmeasure REAL,"
                        + "correlation REAL,\n"
                        + "time BIGINT)");

                stat.execute("CREATE TABLE results (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "res1 TEXT, "
                        + "prop TEXT, "
                        + "res2 TEXT, "
                        + "lens TEXT, "
                        + "score REAL, "
                        + "valid TEXT,"
                        + "list_order INTEGER,"
                        + "leftRoot TEXT,"
                        + "rightRoot TEXT,"
                        + "leftPath TEXT,"
                        + "rightPath TEXT)");
                stat.execute("CREATE TABLE blocks (res1 TEXT, res2 TEXT)");
                stat.execute("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "stage TEXT,"
                        + "level TEXT,"
                        + "message TEXT)");
            }
            tablesCreated = true;
        }

    }

    public void clearAlignments() throws SQLException {
        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                try (Statement stat = connection.createStatement()) {
                    stat.execute("DELETE FROM results");
                    stat.execute("DELETE FROM blocks");
                    tablesCreated = true;
                }
            }
        }

    }

    private void saveStats(Connection connection, Run run) throws SQLException {
        try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO runs(identifier,configName,datasetName,precision,recall,fmeasure,correlation,time) VALUES (?,?,?,?,?,?,?,?)")) {
            pstat.setString(1, run.identifier);
            pstat.setString(2, run.configName);
            pstat.setString(3, run.datasetName);
            pstat.setDouble(4, run.precision);
            pstat.setDouble(5, run.recall);
            pstat.setDouble(6, run.fmeasure);
            pstat.setDouble(7, run.correlation);
            pstat.setLong(8, run.time);
            pstat.execute();
        }
    }

    private void saveResults(Connection connection, DataView dataView) throws SQLException, JsonProcessingException {
        //alignmentSet.sortAlignments();
        ObjectMapper mapper = new ObjectMapper();
        connection.setAutoCommit(false);
        try (Statement stat = connection.createStatement()) {
            stat.execute("UPDATE results SET list_order = id WHERE list_order = 0");
        }
        try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO results(res1,prop,res2,lens,score,valid,leftRoot,rightRoot,leftPath,rightPath) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            for (DataViewEntry dve : dataView.entries) {
                for (DataViewPath dvp : dve.paths) {

                    Map<String, LangStringPair> m = lensResults.get(new Pair(dvp.alignment.entity1, dvp.alignment.entity2));
                    if (m == null) {
                        m = createLensResult(dvp.alignment.entity1, dvp.alignment.entity2);
                    }
                    String lens = m == null ? "{}" : mapper.writeValueAsString(m);
                    pstat.setString(1, dvp.alignment.entity1.getURI());
                    pstat.setString(2, dvp.alignment.relation);
                    pstat.setString(3, dvp.alignment.entity2.getURI());
                    pstat.setString(4, lens);
                    pstat.setDouble(5, dvp.alignment.score);
                    pstat.setString(6, dvp.alignment.valid.toString());
                    pstat.setString(7, dve.root);
                    pstat.setString(8, dvp.rightRoot);
                    pstat.setString(9, mapper.writeValueAsString(dvp.leftPath));
                    pstat.setString(10, mapper.writeValueAsString(dvp.rightPath));
                    pstat.execute();
                }
            }
            connection.commit();
            pstat.close();
        }
    }

    public static void saveBlocks(Connection connection, List<Pair<Resource, Resource>> blocks) throws SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO blocks(res1, res2) VALUES (?,?)")) {
            int i = 0;
            for (Pair<Resource, Resource> block : blocks) {
                if (block == null) {
                    throw new RuntimeException();
                }
                if (block._1 == null || !block._1.isURIResource()
                        || block._2 == null || !block._2.isURIResource()) {
                    System.err.println("Bad block generated");
                    continue;
                }
                pstat.setString(1, block._1.getURI());
                pstat.setString(2, block._2.getURI());
                pstat.execute();
                if (++i % 1000 == 0) {
                    connection.commit();
                }
            }
        }
        connection.commit();
        blocks.clear();
    }

    public void saveAligment(Run run, AlignmentSet alignmentSet, Dataset left, Dataset right) {
        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                createTables(connection);
                saveStats(connection, run);
                DataView dataView = DataView.build(left, right, alignmentSet);
                saveResults(connection, dataView);
                saveBlocks(connection, blocks);
                saveMessages(connection, messages);
            } catch (SQLException | JsonProcessingException x) {
                throw new RuntimeException(x);
            }
        }
    }

    public void updateAlignment(Run run, List<RunResultRow> rrrs) {
        ObjectMapper mapper = new ObjectMapper();
        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                try (Statement stat = connection.createStatement()) {
                    try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO runs(identifier,configName,datasetName,precision,recall,fmeasure,correlation,time) VALUES (?,?,?,?,?,?,?,?)")) {
                        pstat.setString(1, run.identifier);
                        pstat.setString(2, run.configName);
                        pstat.setString(3, run.datasetName);
                        pstat.setDouble(4, run.precision);
                        pstat.setDouble(5, run.recall);
                        pstat.setDouble(6, run.fmeasure);
                        pstat.setDouble(7, run.correlation);
                        pstat.setLong(8, run.time);
                        pstat.execute();
                        pstat.close();
                        stat.execute("DELETE FROM results");
                    }
                    try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO results(res1,prop,res2,lens,score,valid) VALUES (?,?,?,?,?,?)")) {

                        for (RunResultRow rrr : rrrs) {
                            String lens = rrr.lens == null ? "{}" : mapper.writeValueAsString(rrr.lens);
                            pstat.setString(1, rrr.subject);
                            pstat.setString(2, rrr.property);
                            pstat.setString(3, rrr.object);
                            pstat.setString(4, lens);
                            pstat.setDouble(5, rrr.score);
                            pstat.setString(6, rrr.valid.toString());
                            pstat.execute();
                        }
                    }
                }
            } catch (SQLException | JsonProcessingException x) {
                throw new RuntimeException(x);
            }
        }

    }

    public static Run loadRun(String id) {
        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                try (Statement stat = connection.createStatement()) {
                    try (ResultSet rs = stat.executeQuery("SELECT identifier,configName,datasetName,precision,recall,fmeasure,correlation,time FROM runs")) {
                        Run r = null;
                        while (rs.next()) {
                            r = new Run();
                            r.identifier = rs.getString(1);
                            r.configName = rs.getString(2);
                            r.datasetName = rs.getString(3);
                            r.precision = rs.getDouble(4);
                            r.recall = rs.getDouble(5);
                            r.fmeasure = rs.getDouble(6);
                            r.correlation = rs.getDouble(7);
                            r.time = rs.getLong(8);
                        }

                        return r;
                    }
                }
            } catch (SQLException x) {
                throw new RuntimeException(x);
            }
        }

    }

    public static List<RunResultRow> loadData(String id) {
        return loadData(id, -1, -1);
    }

    public static List<RunResultRow> loadData(String id, int offset, int limit) {
        ObjectMapper mapper = new ObjectMapper();
        if (!new File("runs/" + id + ".db").exists()) {
            return null;
        }
        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                List<RunResultRow> rrrs = new ArrayList<>();
                try (Statement stat = connection.createStatement()) {
                    final String query;
                    if (offset >= 0 && limit > 0) {
                        query = "SELECT res1, prop, res2, lens, score, valid, id, leftRoot, rightRoot, leftPath, rightPath FROM results ORDER BY list_order LIMIT " + limit + " OFFSET " + offset;
                    } else {
                        query = "SELECT res1, prop, res2, lens, score, valid, id, leftRoot, rightRoot, leftPath, rightPath FROM results ORDER BY list_order";
                    }
                    try (ResultSet rs = stat.executeQuery(query)) {
                        while (rs.next()) {
                            RunResultRow rrr = new RunResultRow();
                            rrr.subject = rs.getString(1);
                            rrr.property = rs.getString(2);
                            rrr.object = rs.getString(3);
                            rrr.lens = mapper.readValue(rs.getString(4), mapper.getTypeFactory().constructMapType(Map.class, String.class, LangStringPair.class));
                            rrr.score = rs.getDouble(5);
                            rrr.valid = Valid.valueOf(rs.getString(6));
                            rrr.idx = rs.getInt(7);
                            rrr.leftRoot = rs.getString(8);
                            rrr.rightRoot = rs.getString(9);
                            rrr.leftPath = mapper.readValue(rs.getString(10), mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                            rrr.rightPath = mapper.readValue(rs.getString(11), mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                            rrrs.add(rrr);
                        }
                    }
                }
                simplifyView(rrrs);
                return rrrs;
            } catch (SQLException | IOException x) {
                throw new RuntimeException(x);
            }
        }
    }

    private static void simplifyView(List<RunResultRow> rrrs) {
        String lastLeftRoot = "";
        String lastRightRoot = "";
        List<String> lastLeftPath = Collections.EMPTY_LIST;
        List<String> lastRightPath = Collections.EMPTY_LIST;
        for (RunResultRow rrr : rrrs) {
            if (rrr.leftRoot.equals(lastLeftRoot)) {
                rrr.leftRoot = "";
                List<String> newLeftPath = new ArrayList<>();
                Iterator<String> i1 = rrr.leftPath.iterator();
                Iterator<String> i2 = lastLeftPath.iterator();
                while (i1.hasNext() && i2.hasNext()) {
                    String s = i1.next();
                    if (s.equals(i2.next())) {
                        newLeftPath.add("");
                    } else {
                        newLeftPath.add(s);
                    }
                }
                while (i1.hasNext()) {
                    newLeftPath.add(i1.next());
                }
                lastLeftPath = rrr.leftPath;
                rrr.leftPath = newLeftPath;
            } else {
                lastLeftRoot = rrr.leftRoot;
            }
            if (rrr.rightRoot.equals(lastRightRoot)) {
                rrr.rightRoot = "";
                List<String> newRightPath = new ArrayList<>();
                Iterator<String> i1 = rrr.leftPath.iterator();
                Iterator<String> i2 = lastRightPath.iterator();
                while (i1.hasNext() && i2.hasNext()) {
                    String s = i1.next();
                    if (s.equals(i2.next())) {
                        newRightPath.add("");
                    } else {
                        newRightPath.add(s);
                    }
                }
                while (i1.hasNext()) {
                    newRightPath.add(i1.next());
                }
                lastRightPath = rrr.leftPath;
                rrr.leftPath = newRightPath;
            } else {
                lastRightRoot = rrr.rightRoot;
            }
        }
    }

    public static int truePositives(String id) {
        return count(id, "yes");
    }

    public static int falsePositives(String id) {
        return count(id, "no");
    }

    public static int falseNegatives(String id) {
        return count(id, "novel");
    }

    private static int count(String id, String what) {
        if (!new File("runs/" + id + ".db").exists()) {
            return -1;
        }
        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                List<RunResultRow> rrrs = new ArrayList<>();
                try (Statement stat = connection.createStatement()) {
                    try (ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM results WHERE valid == '" + what + "'")) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException x) {
                x.printStackTrace();
            }
        }
        return -1;
    }

    public static int noResults(String id) {
        if (!new File("runs/" + id + ".db").exists()) {
            return -1;
        }
        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                List<RunResultRow> rrrs = new ArrayList<>();
                try (Statement stat = connection.createStatement()) {
                    try (ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM results")) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException x) {
                x.printStackTrace();
            }
        }
        return -1;
    }

    public int addAlignment(Run run, int idx, String subject, String property, String object) {
        final int max;
        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                connection.setAutoCommit(false);
                try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO runs(identifier,configName,datasetName,precision,recall,fmeasure,correlation,time) VALUES (?,?,?,?,?,?,?,?)")) {
                    pstat.setString(1, run.identifier);
                    pstat.setString(2, run.configName);
                    pstat.setString(3, run.datasetName);
                    pstat.setDouble(4, run.precision);
                    pstat.setDouble(5, run.recall);
                    pstat.setDouble(6, run.fmeasure);
                    pstat.setDouble(7, run.correlation);
                    pstat.setLong(8, run.time);
                    pstat.execute();
                }

                try (Statement stat = connection.createStatement()) {
                    final ResultSet res = stat.executeQuery("SELECT max(id) FROM results");
                    max = res.getInt(1);
                }
                connection.commit();

                try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO results(res1,prop,res2,lens,score,valid,list_order) VALUES (?,?,?,?,?,?,?)")) {

                    String lens = "{}";
                    pstat.setString(1, subject);
                    pstat.setString(2, property);
                    pstat.setString(3, object);
                    pstat.setString(4, lens);
                    pstat.setDouble(5, 1.0);
                    pstat.setString(6, "novel");
                    pstat.setInt(7, idx);
                    pstat.execute();
                    pstat.close();
                }
                connection.commit();
            } catch (SQLException x) {
                throw new RuntimeException(x);
            }
        }
        return max;
    }

    public void removeAlignment(Run run, int idx) {

        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO runs(identifier,configName,datasetName,precision,recall,fmeasure,correlation,time) VALUES (?,?,?,?,?,?,?,?)")) {
                    pstat.setString(1, run.identifier);
                    pstat.setString(2, run.configName);
                    pstat.setString(3, run.datasetName);
                    pstat.setDouble(4, run.precision);
                    pstat.setDouble(5, run.recall);
                    pstat.setDouble(6, run.fmeasure);
                    pstat.setDouble(7, run.correlation);
                    pstat.setLong(8, run.time);
                    pstat.execute();
                }

                try (Statement stat = connection.createStatement()) {
                    stat.execute("DELETE FROM results WHERE id=" + idx);
                }
            } catch (SQLException x) {
                throw new RuntimeException(x);
            }
        }
    }

    public void changeStatus(Run run, int idx, Valid valid) {

        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO runs(identifier,configName,datasetName,precision,recall,fmeasure,correlation,time) VALUES (?,?,?,?,?,?,?,?)")) {
                    pstat.setString(1, run.identifier);
                    pstat.setString(2, run.configName);
                    pstat.setString(3, run.datasetName);
                    pstat.setDouble(4, run.precision);
                    pstat.setDouble(5, run.recall);
                    pstat.setDouble(6, run.fmeasure);
                    pstat.setDouble(7, run.correlation);
                    pstat.setLong(8, run.time);
                    pstat.execute();
                }

                try (PreparedStatement stat = connection.prepareStatement("UPDATE results SET valid=? WHERE id=?")) {
                    stat.setString(1, valid.toString());
                    stat.setInt(2, idx);
                    stat.execute();
                }
            } catch (SQLException x) {
                throw new RuntimeException(x);
            }
        }
    }

    public List<Pair<String, Map<String, String>>> getAlternatives(String entityid, boolean left) {

        ObjectMapper mapper = new ObjectMapper();
        final MapType mapType = mapper.getTypeFactory().constructMapType(Map.class, String.class, LangStringPair.class);

        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                try (PreparedStatement pstat = connection.prepareStatement("SELECT DISTINCT blocks.res"
                        + (left ? "2" : "1") + ", results.lens FROM blocks JOIN results ON blocks.res"
                        + (left ? "2" : "1") + "=results.res"
                        + (left ? "2" : "1") + " WHERE blocks.res"
                        + (left ? "1" : "2") + "=?")) {
                    pstat.setString(1, entityid);
                    try (ResultSet rs = pstat.executeQuery()) {
                        List<Pair<String, Map<String, String>>> result = new ArrayList<>();
                        RESULTS:
                        while (rs.next()) {
                            String altId = rs.getString(1);
                            for (Pair<String, Map<String, String>> p : result) {
                                if (p._1.equals(altId)) {
                                    continue RESULTS;
                                }
                            }
                            Map<String, LangStringPair> lens = mapper.readValue(rs.getString(2), mapType);
                            Map<String, String> lensMap = new HashMap<>();
                            for (Map.Entry<String, LangStringPair> e : lens.entrySet()) {
                                lensMap.put(e.getKey(), left ? e.getValue()._2 : e.getValue()._1);
                            }
                            result.add(new Pair<>(altId, lensMap));
                        }
                        return result;
                    }

                }
            } catch (SQLException | IOException x) {
                x.printStackTrace();
                throw new RuntimeException(x);
            }
        }
    }

    private void saveMessages(Connection connection, List<Message> messages) throws SQLException {
        try (PreparedStatement stat = connection.prepareStatement("INSERT INTO messages(stage,level,message) VALUES (?,?,?)")) {
            for (Message message : messages) {
                stat.setString(1, message.stage.name());
                stat.setString(2, message.level.name());
                stat.setString(3, message.message);
                stat.execute();
            }
            connection.commit();
        }
    }

    public static List<Message> getMessages(String id) {
        List<Message> messages = new ArrayList<>();
        synchronized (databaseLock) {
            try (Connection connection = connection(id)) {
                try (Statement stat = connection.createStatement()) {
                    try (ResultSet rs = stat.executeQuery("SELECT stage, level, message FROM messages")) {
                        while (rs.next()) {
                            messages.add(new Message(Stage.valueOf(rs.getString(1)), Level.valueOf(rs.getString(2)), rs.getString(3)));
                        }
                    }
                }
            } catch (SQLException x) {
                x.printStackTrace();
                throw new RuntimeException(x);
            }
        }
        return messages;
    }

    private Map<String, LangStringPair> createLensResult(Resource entity1, Resource entity2) {
        if (leftLensResults.containsKey(entity1)) {
            Map<String, LangStringPair> merged = new HashMap<>(leftLensResults.get(entity1));
            Map<String, LangStringPair> r = rightLensResults.get(entity2);
            for (Map.Entry<String, LangStringPair> e : merged.entrySet()) {
                if (r != null && r.containsKey(e.getKey())) {
                    e.setValue(new LangStringPair(e.getValue().lang1, r.get(e.getKey()).lang2, e.getValue()._1, r.get(e.getKey())._2));
                } else {
                    e.setValue(new LangStringPair(e.getValue().lang1, Language.UNDEFINED, e.getValue()._1, ""));
                }
            }
            if (r != null) {
                for (Map.Entry<String, LangStringPair> e : r.entrySet()) {
                    if (!merged.containsKey(e.getKey())) {
                        merged.put(e.getKey(), new LangStringPair(Language.UNDEFINED, e.getValue().lang2, "", e.getValue()._2));
                    }
                }
            }
            return merged;
        } else if (rightLensResults.containsKey(entity2)) {
            Map<String, LangStringPair> merged = new HashMap<>(rightLensResults.get(entity2));
            for (Map.Entry<String, LangStringPair> e : merged.entrySet()) {
                e.setValue(new LangStringPair(Language.UNDEFINED, e.getValue().lang2, "", e.getValue()._2));
            }
            return merged;
        } else {
            return null;
        }
    }

    public static class ListenerResponse {

        public NaiscListener.Stage stage = NaiscListener.Stage.INITIALIZING;
        public String lastMessage = "";
    }

    public static class Message {

        public final NaiscListener.Stage stage;
        public final NaiscListener.Level level;
        public final String message;

        public Message(Stage stage, Level level, String message) {
            this.stage = stage;
            this.level = level;
            this.message = message;
        }
    }

}
