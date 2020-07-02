package org.insightcentre.uld.naisc.meas;

import org.insightcentre.uld.naisc.meas.execution.Execution;
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
import org.insightcentre.uld.naisc.main.Configuration;
import static org.insightcentre.uld.naisc.meas.ExecuteServlet.VALID_ID;
import org.insightcentre.uld.naisc.meas.Meas.Run;
import org.insightcentre.uld.naisc.meas.execution.Execution.Message;

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
                    resp.setContentType("application/n-triples; charset=utf-8");
                    try (PrintWriter out = resp.getWriter()) {
                        for (Meas.RunResultRow rrr : data) {
                            if (rrr.valid != Valid.novel) {
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
                    resp.setContentType("application/n-triples; charset=utf-8");
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
        } else if (path.equals("/results")) {
            try {
                String id = req.getParameter("id");
                int offset = Integer.parseInt(req.getParameter("offset"));
                int limit = Integer.parseInt(req.getParameter("limit"));
                resp.setContentType("application/json");
                try (PrintWriter out = resp.getWriter()) {
                    out.println(Meas.loadRunResult(id, offset, limit));
                }
            } catch (IOException | NumberFormatException x) {
                throw new ServletException(x);
            }
        } else if (path.equals("/alternatives")) {
            try {
                String leftId = req.getParameter("left");
                String rightId = req.getParameter("right");
                String executionId = req.getParameter("id");
                if (executionId != null) {
                    resp.setContentType("application/json");
                    if (leftId != null) {
                        try (PrintWriter out = resp.getWriter()) {
                            out.println(mapper.writeValueAsString(new Execution(executionId).getAlternatives(leftId, true)));
                        }
                    } else if (rightId != null) {
                        try (PrintWriter out = resp.getWriter()) {
                            out.println(mapper.writeValueAsString(new Execution(executionId).getAlternatives(rightId, false)));
                        }
                    } else {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No query");
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No run id");
                }
            } catch (IOException | NumberFormatException x) {
                x.printStackTrace();
                throw new ServletException(x);
            }
        } else if (path.equals("/messages")) {
            try {
                String id = req.getParameter("id");
                if (id != null) {
                    List<Message> messages = Execution.getMessages(id);
                    resp.setContentType("application/json");
                    try (PrintWriter out = resp.getWriter()) {
                        out.println(mapper.writeValueAsString(messages));
                    }

                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No run id");
                }
            } catch (IOException x) {
                x.printStackTrace();
                throw new ServletException(x);
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
                    int newId = new Execution(id).addAlignment(run, data.idx, data.subject, data.property, data.object);
                    try (PrintWriter out = resp.getWriter()) {
                        out.println(newId);
                    }
                }
            }
        } else if (path.startsWith("/remove/")) {
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
        } else if (path.startsWith("/update/")) {
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
        } else if (path.startsWith("/save_config/")) {
            try {
                String configId = path.substring("/save_config/".length());
                Configuration config = mapper.readValue(req.getInputStream(), Configuration.class);
                try (PrintWriter out = new PrintWriter("configs/" + configId + ".json")) {
                    mapper.writeValue(out, config);
                }
            } catch (IOException x) {
                x.printStackTrace();
                throw new ServletException(x);
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

        @Override
        public String toString() {
            return "AddRemoveData{" + "idx=" + idx + ", subject=" + subject + ", property=" + property + ", object=" + object + ", valid=" + valid + ", data=" + data + '}';
        }

    }

}
