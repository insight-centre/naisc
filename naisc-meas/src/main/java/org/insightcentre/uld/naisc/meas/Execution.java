package org.insightcentre.uld.naisc.meas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public Execution(String id) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.id = id;
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

    public void saveAligment(Run run, AlignmentSet alignmentSet) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite:runs/" + id + ".db");
            Statement stat = connection.createStatement();
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
            stat.execute("CREATE TABLE results (res1 TEXT, prop TEXT, res2 TEXT, lens TEXT, score REAL, valid TEXT)");
            pstat.close();
            connection.setAutoCommit(false);
            pstat = connection.prepareStatement("INSERT INTO results VALUES (?,?,?,?,?,?)");
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
            stat.close();
            connection.close();
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
            pstat = connection.prepareStatement("INSERT INTO results VALUES (?,?,?,?,?,?)");

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
        ObjectMapper mapper = new ObjectMapper();
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

    public static class ListenerResponse {

        public ExecuteListener.Stage stage = ExecuteListener.Stage.INITIALIZING;
        public String lastMessage = "";
    }

}
