package org.insightcentre.uld.naisc.meas;

import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.CrossFold;
import org.insightcentre.uld.naisc.meas.execution.ExecutionMode;
import org.insightcentre.uld.naisc.meas.execution.ExecutionTask;
import org.insightcentre.uld.naisc.meas.execution.Execution;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.Alignment.Valid;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.meas.Meas.ActiveRun;
import org.insightcentre.uld.naisc.meas.Meas.Run;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 *
 * @author John McCrae
 */
public class ExecuteServlet extends HttpServlet {

    private static final HashMap<String, ExecutionTask> executions = new HashMap<>();
    ObjectMapper mapper = new ObjectMapper();

    final static public String VALID_ID = "[A-Za-z0-9][A-Za-z0-9_\\-]*";

    private static String getURL(HttpServletRequest req) {

        String scheme = req.getScheme();             // http
        String serverName = req.getServerName();     // hostname.com
        int serverPort = req.getServerPort();        // 80
        String contextPath = req.getContextPath();   // /mywebapp

        // Reconstruct original requesting URL
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath);

        return url.toString();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path != null && (path.equals("/start") || path.equals("/train") || path.equals("/crossfold"))) {
            try (BufferedReader r = req.getReader()) {
                ExecuteRequest er = mapper.readValue(r, ExecuteRequest.class);
                String id = er.runId == null || !er.runId.matches(VALID_ID)
                        ? String.format("%016x", new Random().nextLong())
                        : er.runId;
                if (executions.containsKey(id) && !executions.get(id).isActive && !ManageServlet.completed.containsKey(id)) {
                    executions.remove(id);
                } else if (ManageServlet.completed.containsKey(id) || executions.containsKey(id)) {
                    throw new IllegalArgumentException("Run already exists!");
                }
                final ExecutionMode mode;
                if (path.equals("/start")) {
                    mode = ExecutionMode.EVALUATE;
                } else if (path.equals("/train")) {
                    mode = ExecutionMode.TRAIN;
                } else if (path.equals("/crossfold")) {
                    mode = ExecutionMode.CROSSFOLD;
                } else {
                    throw new RuntimeException("Unreachable");
                }
                ExecutionTask execution = new ExecutionTask(er.config, er.configName, er.dataset, id, mode,
                        getURL(req), new None<>(), er.foldCount, er.foldDir);
                executions.put(id, execution);
                Thread t = new Thread(execution);
                t.start();
                try (PrintWriter out = resp.getWriter()) {
                    out.print(id);
                }
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path != null && path.startsWith("/status/") && executions.containsKey(path.substring(8))) {
            try (PrintWriter out = resp.getWriter()) {
                mapper.writeValue(out, executions.get(path.substring(8)).listener.response);
            }
        } else if (path != null && path.startsWith("/kill/") && executions.containsKey(path.substring(6))) {
            executions.get(path.substring(6)).listener.aborted = true;
        } else if (path != null && path.startsWith("/completed/") && executions.containsKey(path.substring(11))) {
            try (PrintWriter out = resp.getWriter()) {
                mapper.writeValue(out, ManageServlet.completed.get(path.substring(11)));
            }
        } else if (path != null && (path.startsWith("/rerun/") || path.startsWith("/retrain/"))) {
            String id = path.substring(path.startsWith("/retrain/") ? 9 : 7);
            if (id.matches(VALID_ID)) {
                Run r = ManageServlet.completed.containsKey(id)
                        ? ManageServlet.completed.get(id)
                        : Execution.loadRun(id);

                File configFile = new File("configs/" + r.configName + ".json");
                if (!configFile.exists()) {
                    throw new ServletException("The configuration was removed, cannot rerun");
                }
                Configuration config = mapper.readValue(configFile, Configuration.class);

                AlignmentSet gold = convertToAlignmentSet(Execution.loadData(id));

                ExecutionTask execution = new ExecutionTask(config, r.configName, r.datasetName, id,
                        path.startsWith("/retrain/") ? ExecutionMode.TRAIN : ExecutionMode.EVALUATE,
                        getURL(req), new Some<>(gold), -1, null);
                executions.put(id, execution);
                Thread t = new Thread(execution);
                t.start();
                Iterator<Run> riter = Meas.data.runs.iterator();
                while(riter.hasNext()) {
                    if(riter.next().identifier.equals(id)) {
                        riter.remove();
                    }
                }
                try (PrintWriter out = resp.getWriter()) {
                    out.print(id);
                }
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private AlignmentSet convertToAlignmentSet(List<Meas.RunResultRow> data) {
        AlignmentSet as = new AlignmentSet();
        Model model = ModelFactory.createDefaultModel();
        for (Meas.RunResultRow rrr : data) {
            if (rrr.valid == Valid.yes || rrr.valid == Valid.novel) {
                as.add(new Alignment(new URIRes(rrr.subject, "left"), new URIRes(rrr.object, "right"), rrr.score, rrr.property, null));
            }
        }
        return as;
    }

    private static class ExecuteRequest {

        public Configuration config;
        public String configName;
        public String dataset;
        public String runId;
        public int foldCount = 0;
        public CrossFold.FoldDirection foldDir;
    }

    /**
     * Get all the runs that are in progress at the moment
     *
     * @return The list of active runs
     */
    public static List<ActiveRun> activeRuns() {
        List<ActiveRun> runs = new ArrayList<>();
        for (ExecutionTask et : executions.values()) {
            if (et.isActive) {
                runs.add(new ActiveRun(et.id, et.configName, et.dataset, et.listener.response.stage, et.listener.response.lastMessage, true));
            }
        }
        return runs;
    }


}
