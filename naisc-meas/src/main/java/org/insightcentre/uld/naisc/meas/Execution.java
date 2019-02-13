package org.insightcentre.uld.naisc.meas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.Alignment.Valid;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.main.ExecuteListener;
import org.insightcentre.uld.naisc.meas.Meas.Run;
import org.insightcentre.uld.naisc.meas.Meas.RunResultRow;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.Pair;

/**
 *
 * @author John McCrae
 */
public class Execution implements ExecuteListener {

    ListenerResponse response = new ListenerResponse();
    boolean aborted = false;
    private final String id;
    private final HashMap<Pair<String, String>, Map<String, LangStringPair>> lensResults = new HashMap<>();
    private final List<Pair<Resource, Resource>> blocks = new ArrayList<>();

    public Execution(String id) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.id = id;
            assert (id.matches(ExecuteServlet.VALID_ID));
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
        Pair<String, String> p = new Pair(id1.getURI(), id2.getURI());
        if (!lensResults.containsKey(p)) {
            lensResults.put(p, new HashMap<>());
        }
        lensResults.get(p).put(lensId, res);
    }

    @Override
    public void addBlock(Resource res1, Resource res2) {
        blocks.add(new Pair<>(res1, res2));
        if (blocks.size() > 100000) {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db")) {
                createTables(connection);
                saveBlocks(connection);
            } catch (SQLException x) {
                x.printStackTrace();
            }
        }
    }

    private boolean tablesCreated = false;

    private void createTables(Connection connection) throws SQLException {
        if (!tablesCreated) {
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

                stat.execute("CREATE TABLE results (id INTEGER PRIMARY KEY AUTOINCREMENT, res1 TEXT, prop TEXT, res2 TEXT, lens TEXT, score REAL, valid TEXT)");
                stat.execute("CREATE TABLE blocks (res1 TEXT, res2 TEXT)");
            }
            tablesCreated = true;
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

    private void saveResults(Connection connection, AlignmentSet alignmentSet) throws SQLException, JsonProcessingException {
        alignmentSet.sortAlignments();
        ObjectMapper mapper = new ObjectMapper();
        connection.setAutoCommit(false);
        try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO results(res1,prop,res2,lens,score,valid) VALUES (?,?,?,?,?,?)")) {
            for (Alignment alignment : alignmentSet) {
                Map<String, LangStringPair> m = lensResults.get(new Pair(alignment.entity1, alignment.entity2));
                String lens = m == null ? "{}" : mapper.writeValueAsString(m);
                pstat.setString(1, alignment.entity1);
                pstat.setString(2, alignment.relation);
                pstat.setString(3, alignment.entity2);
                pstat.setString(4, lens);
                pstat.setDouble(5, alignment.score);
                pstat.setString(6, alignment.valid.toString());
                pstat.execute();
            }
            connection.commit();
            pstat.close();
        }
    }

    public void saveBlocks(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO blocks(res1, res2) VALUES (?,?)")) {
            for (Pair<Resource, Resource> block : blocks) {
                pstat.setString(1, block._1.getURI());
                pstat.setString(2, block._2.getURI());
                pstat.execute();
            }
        }
        connection.commit();
        blocks.clear();
    }

    public void saveAligment(Run run, AlignmentSet alignmentSet) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db")) {
            createTables(connection);
            saveStats(connection, run);
            saveResults(connection, alignmentSet);
            saveBlocks(connection);
        } catch (SQLException | JsonProcessingException x) {
            throw new RuntimeException(x);
        }
    }

    public void updateAlignment(Run run, List<RunResultRow> rrrs) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db");
            Statement stat = connection.createStatement();
            PreparedStatement pstat = connection.prepareStatement("INSERT INTO runs(identifier,configName,datasetName,precision,recall,fmeasure,correlation,time) VALUES (?,?,?,?,?,?,?,?)");
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
            pstat = connection.prepareStatement("INSERT INTO results(res1,prop,res2,lens,score,valid) VALUES (?,?,?,?,?,?)");

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
            pstat.close();
            stat.close();
            connection.close();
        } catch (SQLException | JsonProcessingException x) {
            throw new RuntimeException(x);
        }

    }

    public static Run loadRun(String id) {
        try {
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db");
            Statement stat = connection.createStatement();
            ResultSet rs = stat.executeQuery("SELECT identifier,configName,datasetName,precision,recall,fmeasure,correlation,time FROM runs");
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
            rs.close();
            stat.close();
            connection.close();
            return r;
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }

    }

    public static List<RunResultRow> loadData(String id) {
        ObjectMapper mapper = new ObjectMapper();
        if (!new File("runs/" + id + ".db").exists()) {
            return null;
        }
        try {
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db");
            Statement stat = connection.createStatement();
            ResultSet rs = stat.executeQuery("SELECT res1, prop, res2, lens, score, valid FROM results");
            List<RunResultRow> rrrs = new ArrayList<>();
            while (rs.next()) {
                RunResultRow rrr = new RunResultRow();
                rrr.subject = rs.getString(1);
                rrr.property = rs.getString(2);
                rrr.object = rs.getString(3);
                rrr.lens = mapper.readValue(rs.getString(4), mapper.getTypeFactory().constructMapType(Map.class, String.class, LangStringPair.class));
                rrr.score = rs.getDouble(5);
                rrr.valid = Valid.valueOf(rs.getString(6));
                rrrs.add(rrr);
            }
            rs.close();
            stat.close();
            connection.close();
            return rrrs;
        } catch (SQLException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    public static List<RunResultRow> loadData(String id, int offset, int limit) {
        ObjectMapper mapper = new ObjectMapper();
        if (!new File("runs/" + id + ".db").exists()) {
            return null;
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db")) {
            List<RunResultRow> rrrs = new ArrayList<>();
            try (Statement stat = connection.createStatement()) {
                try (ResultSet rs = stat.executeQuery("SELECT res1, prop, res2, lens, score, valid, id FROM results LIMIT " + limit + " OFFSET " + offset)) {
                    while (rs.next()) {
                        RunResultRow rrr = new RunResultRow();
                        rrr.subject = rs.getString(1);
                        rrr.property = rs.getString(2);
                        rrr.object = rs.getString(3);
                        rrr.lens = mapper.readValue(rs.getString(4), mapper.getTypeFactory().constructMapType(Map.class, String.class, LangStringPair.class));
                        rrr.score = rs.getDouble(5);
                        rrr.valid = Valid.valueOf(rs.getString(6));
                        rrr.idx = rs.getInt(7);
                        rrrs.add(rrr);
                    }
                }
            }
            return rrrs;
        } catch (SQLException | IOException x) {
            throw new RuntimeException(x);
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
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db")) {
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
        return -1;
    }

    public static int noResults(String id) {
        if (!new File("runs/" + id + ".db").exists()) {
            return -1;
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db")) {
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
        return -1;
    }

    public void addAlignment(Run run, int idx, String subject, String property, String object) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db")) {
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
                int max = res.getInt(1);
                for (int i = max; i >= idx + 2; i--) {
                    stat.execute("UPDATE results SET id = id + 1 WHERE id = " + i);
                }
            }

            try (PreparedStatement pstat = connection.prepareStatement("INSERT INTO results(id,res1,prop,res2,lens,score,valid) VALUES (?,?,?,?,?,?,?)")) {

                String lens = "{}";
                pstat.setInt(1, idx + 2);
                pstat.setString(2, subject);
                pstat.setString(3, property);
                pstat.setString(4, object);
                pstat.setString(5, lens);
                pstat.setDouble(6, 1.0);
                pstat.setString(7, "novel");
                pstat.execute();
                pstat.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    void removeAlignment(Run run, int idx) {

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db")) {
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
                System.err.println(idx);
                stat.execute("DELETE FROM results WHERE id=" + (idx + 1));
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    void changeStatus(Run run, int idx, Valid valid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db")) {
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
                stat.setInt(2, idx + 1);
                stat.execute();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    List<Pair<String, Map<String, String>>> getAlternatives(String entityid, boolean left) {

        ObjectMapper mapper = new ObjectMapper();
        final MapType mapType = mapper.getTypeFactory().constructMapType(Map.class, String.class, LangStringPair.class);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db")) {
            try (PreparedStatement pstat = connection.prepareStatement("SELECT DISTINCT blocks.res"
                    + (left ? "2" : "1") + ", results.lens FROM blocks JOIN results ON blocks.res"
                    + (left ? "2" : "1") + "=results.res"
                    + (left ? "2" : "1") + " WHERE blocks.res"
                    + (left ? "1" : "2") + "=?")) {
                pstat.setString(1, entityid);
                try (ResultSet rs = pstat.executeQuery()) {
                    List<Pair<String, Map<String, String>>> result = new ArrayList<>();
                    RESULTS: while (rs.next()) {
                        String altId = rs.getString(1);
                        for(Pair<String, Map<String, String>> p : result) {
                            if(p._1.equals(altId))
                                continue RESULTS;
                        }
                        Map<String, LangStringPair> lens = mapper.readValue(rs.getString(2), mapType);
                        Map<String, String> lensMap = new HashMap<>();
                        for(Map.Entry<String, LangStringPair> e : lens.entrySet()) {
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

    public static class ListenerResponse {

        public ExecuteListener.Stage stage = ExecuteListener.Stage.INITIALIZING;
        public String lastMessage = "";
    }

}
