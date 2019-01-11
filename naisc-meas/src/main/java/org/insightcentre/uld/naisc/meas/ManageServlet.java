package org.insightcentre.uld.naisc.meas;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.insightcentre.uld.naisc.Alignment.Valid;
import static org.insightcentre.uld.naisc.meas.ExecuteServlet.VALID_ID;
import org.insightcentre.uld.naisc.meas.Meas.Run;

/**
 *
 * @author John McCrae
 */
public class ManageServlet extends HttpServlet {

    public static Map<String, Meas.Run> completed = Collections.synchronizedMap(new HashMap<String, Meas.Run>());
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.err.println(req.getPathInfo());
        String path = req.getPathInfo();
        if (path == null) {
            return;
        }
        if (path.startsWith("/remove/")) {
            String id = path.substring(8);
            if (id.matches(VALID_ID)) {
                try {
                    Iterator<Meas.Run> r = Meas.data.runs.iterator();
                    while (r.hasNext()) {
                        if (r.next().identifier.equals(id)) {
                            r.remove();
                        }
                    }
                    completed.remove(id);
                    File f = new File(new File("runs"), id + ".db");
                    f.delete();
                    System.err.println("Deleted " + f);
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        } else if (path.startsWith("/download_all/")) {
            String id = path.substring(14);
            if (id.matches(VALID_ID)) {
                final List<Meas.RunResultRow> data = Execution.loadData(id);
                if (data != null) {
                    resp.setContentType("application/n-triples");
                    try (PrintWriter out = resp.getWriter()) {
                        for (Meas.RunResultRow rrr : data) {
                            if (rrr.valid == Valid.yes || rrr.valid == Valid.no) {
                                out.printf("<%s> <%s> <%s> . # %.4f\n", rrr.subject, rrr.property, rrr.object, rrr.score);
                            }
                        }
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else if (path.startsWith("/download_valid/")) {
            String id = path.substring(16);
            if (id.matches(VALID_ID)) {
                final List<Meas.RunResultRow> data = Execution.loadData(id);
                if (data != null) {
                    resp.setContentType("application/n-triples");
                    try (PrintWriter out = resp.getWriter()) {
                        for (Meas.RunResultRow rrr : data) {
                            if (rrr.valid == Valid.yes || rrr.valid == Valid.novel) {
                                out.printf("<%s> <%s> <%s> . # %.4f\n", rrr.subject, rrr.property, rrr.object, rrr.score);
                            }
                        }
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.err.println(req.getPathInfo());
        String path = req.getPathInfo();
        if (path.startsWith("/add/")) {
            String id = path.substring(5);
            if (id.matches(VALID_ID)) {
                AddRemoveData data = mapper.readValue(req.getReader(), AddRemoveData.class);
                Run oldRun = getOldRun(data.data, id);
                if (oldRun != null) {
                    Run run = new Meas.Run(data.data.identifier, oldRun.configName, oldRun.datasetName, data.data.precision, data.data.recall, data.data.fmeasure, -2, oldRun.time, oldRun.isTrain);
                    completed.put(data.data.identifier, run);
                    Meas.updateRun(data.data.identifier, run);
                    new Execution(id).addAlignment(run, data.idx, data.subject, data.property, data.object);
                }
            }
        } else if(path.startsWith("/remove/")) {
            String id = path.substring(8);
            if (id.matches(VALID_ID)) {
                AddRemoveData data = mapper.readValue(req.getReader(), AddRemoveData.class);
                Run oldRun = getOldRun(data.data, id);
                if (oldRun != null) {
                    Run run = new Meas.Run(data.data.identifier, oldRun.configName, oldRun.datasetName, data.data.precision, data.data.recall, data.data.fmeasure, -2, oldRun.time, oldRun.isTrain);
                    completed.put(data.data.identifier, run);
                    Meas.updateRun(data.data.identifier, run);
                    new Execution(id).removeAlignment(run, data.idx);
                }
            }
        } else if(path.startsWith("/update/")) {
            String id = path.substring(8);
            if (id.matches(VALID_ID)) {
                AddRemoveData data = mapper.readValue(req.getReader(), AddRemoveData.class);
                Run oldRun = getOldRun(data.data, id);
                if (oldRun != null) {
                    Run run = new Meas.Run(data.data.identifier, oldRun.configName, oldRun.datasetName, data.data.precision, data.data.recall, data.data.fmeasure, -2, oldRun.time, oldRun.isTrain);
                    completed.put(data.data.identifier, run);
                    Meas.updateRun(data.data.identifier, run);
                    new Execution(id).changeStatus(run, data.idx, data.valid);
                }
            }
            
        } else if (path.startsWith("/rerun/")) {
            throw new UnsupportedOperationException("TODO");
        } else if (path.startsWith("/retrain/")) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    private Run getOldRun(SaveData data, String id) {
        Run oldRun = completed.get(data.identifier);
        if (oldRun == null) {
            oldRun = Execution.loadRun(id);
        }
        return oldRun;
    }

    public static class SaveData {

        public String identifier;
        public double precision, recall, fmeasure;
    }

    public static class AddRemoveData {

        public int idx;
        public String subject, property, object;
        public Valid valid;
        public SaveData data;
    }

}
