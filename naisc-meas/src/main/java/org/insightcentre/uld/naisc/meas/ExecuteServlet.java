package org.insightcentre.uld.naisc.meas;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.ExecuteListener.Stage;
import org.insightcentre.uld.naisc.main.Main;
import org.insightcentre.uld.naisc.meas.Meas.Run;

/**
 *
 * @author John McCrae
 */
public class ExecuteServlet extends HttpServlet {

    private HashMap<String, ExecutionTask> executions = new HashMap<>();
    ObjectMapper mapper = new ObjectMapper();

    final static public String VALID_ID = "[A-Za-z][A-Za-z0-9_\\-]*";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (BufferedReader r = req.getReader()) {
            ExecuteRequest er = mapper.readValue(r, ExecuteRequest.class);
            String id = er.runId == null || !er.runId.matches(VALID_ID)
                    ? String.format("%016x", new Random().nextLong())
                    : er.runId;
            if (ManageServlet.completed.containsKey(id) || executions.containsKey(id)) {
                throw new IllegalArgumentException("Run already exists!");
            }
            ExecutionTask execution = new ExecutionTask(er.config, er.configName, er.dataset, id);
            executions.put(id, execution);
            Thread t = new Thread(execution);
            t.start();
            try (PrintWriter out = resp.getWriter()) {
                out.print(id);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.err.println(req.getPathInfo());
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
        } 
    }

    private static class ExecuteRequest {

        public Configuration config;
        public String configName;
        public String dataset;
        public String runId;
    }

    private static class ExecutionTask implements Runnable {

        private final Configuration config;
        private final String dataset, id, configName;
        public final Execution listener;

        public ExecutionTask(Configuration config, String configName, String dataset, String id) {
            this.config = config;
            this.dataset = dataset;
            this.id = id;
            this.configName = configName;
            this.listener = new Execution(id);
        }

        @Override
        public void run() {
            try {
                File f = new File("runs");
                f.mkdirs();
                long time = System.currentTimeMillis();
                AlignmentSet alignment = Main.execute(new File(new File(new File("datasets"), dataset), "left.rdf"),
                        new File(new File(new File("datasets"), dataset), "right.rdf"),
                        config, listener);
                time = System.currentTimeMillis() - time;
                listener.updateStatus(Stage.COMPLETED, "Completed");
                Run run = new Run(id, configName, dataset, -1.0, -1.0, -1.0, -2.0, time);
                ManageServlet.completed.put(id, run);
                listener.saveAligment(run, alignment);
                Meas.data.runs.add(run);
            } catch (Exception x) {
                x.printStackTrace();
                listener.response.stage = Stage.FAILED;
                listener.response.lastMessage = x.getClass().getSimpleName() + ": " + x.getMessage();
            }
        }

    }
}
