package org.insightcentre.uld.naisc.meas;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.Evaluate;
import org.insightcentre.uld.naisc.main.ExecuteListener.Stage;
import org.insightcentre.uld.naisc.main.Main;
import org.insightcentre.uld.naisc.main.Train;
import org.insightcentre.uld.naisc.meas.Meas.Run;

/**
 *
 * @author John McCrae
 */
public class ExecuteServlet extends HttpServlet {

    private HashMap<String, ExecutionTask> executions = new HashMap<>();
    ObjectMapper mapper = new ObjectMapper();

    final static public String VALID_ID = "[A-Za-z0-9][A-Za-z0-9_\\-]*";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (BufferedReader r = req.getReader()) {
            ExecuteRequest er = mapper.readValue(r, ExecuteRequest.class);
            String id = er.runId == null || !er.runId.matches(VALID_ID)
                    ? String.format("%016x", new Random().nextLong())
                    : er.runId;
            if(executions.containsKey(id) && !executions.get(id).isActive && !ManageServlet.completed.containsKey(id)) {
                executions.remove(id);
            } else if (ManageServlet.completed.containsKey(id) || executions.containsKey(id)) {
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
        public boolean isActive;

        public ExecutionTask(Configuration config, String configName, String dataset, String id) {
            this.config = config;
            this.dataset = dataset;
            this.id = id;
            this.configName = configName;
            this.listener = new Execution(id);
            this.isActive = false;
        }

        @Override
        public void run() {
            try {
                isActive = true;
                File f = new File("runs");
                f.mkdirs();
                long time = System.currentTimeMillis();
                AlignmentSet alignment = Main.execute(new File(new File(new File("datasets"), dataset), "left.rdf"),
                        new File(new File(new File("datasets"), dataset), "right.rdf"),
                        config, listener);
                if(alignment == null) return;
                time = System.currentTimeMillis() - time;
                File alignFile = new File(new File(new File("datasets"), dataset), "align.rdf");
                final Evaluate.EvaluationResults er;
                if(alignFile.exists()) {
                    listener.updateStatus(Stage.EVALUATION, "Evaluating");
                    Map<Property, Object2DoubleMap<Statement>> gold = Train.readAlignments(alignFile);
                    er = Evaluate.evaluate(alignment, gold);
                } else {
                    er = null;
                }
                listener.updateStatus(Stage.COMPLETED, "Completed");
                Run run = new Run(id, configName, dataset, 
                        er == null ? -1.0 : er.precision(), 
                        er == null ? -1.0 : er.recall(), 
                        er == null ? -1.0 : er.fmeasure(), 
                        er == null ? -2.0 : er.correlation, time);
                ManageServlet.completed.put(id, run);
                listener.saveAligment(run, alignment);
                Meas.data.runs.add(run);
            } catch (Exception x) {
                x.printStackTrace();
                listener.response.stage = Stage.FAILED;
                listener.response.lastMessage = x.getClass().getSimpleName() + ": " + x.getMessage();
            } finally {
                isActive = false;
            }
        }

    }
}
