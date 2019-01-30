package org.insightcentre.uld.naisc.meas;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.CrossFold;
import org.insightcentre.uld.naisc.main.Evaluate;
import org.insightcentre.uld.naisc.main.ExecuteListener.Stage;
import org.insightcentre.uld.naisc.main.Main;
import org.insightcentre.uld.naisc.main.Train;
import org.insightcentre.uld.naisc.meas.Meas.Run;
import org.insightcentre.uld.naisc.util.Option;

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
                if(path.equals("/start")) {
                    mode = ExecutionMode.EVALUATE;
                } else if(path.equals("/train")) {
                    mode = ExecutionMode.TRAIN;
                } else if(path.equals("/crossfold")) {
                    mode = ExecutionMode.CROSSFOLD;
                } else {
                    throw new RuntimeException("Unreachable");
                }
                ExecutionTask execution = new ExecutionTask(er.config, er.configName, er.dataset, id, mode);
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
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    private static class ExecuteRequest {
        
        public Configuration config;
        public String configName;
        public String dataset;
        public String runId;
    }
    
    private static enum ExecutionMode {
        EVALUATE,
        TRAIN,
        CROSSFOLD
    }
    
    private static class ExecutionTask implements Runnable {
        
        private final Configuration config;
        private final String dataset, id, configName;
        public final Execution listener;
        public boolean isActive;
        private final ExecutionMode mode;
        
        public ExecutionTask(Configuration config, String configName, String dataset, String id, ExecutionMode mode) {
            this.config = config;
            this.dataset = dataset;
            this.id = id;
            this.configName = configName;
            this.listener = new Execution(id);
            this.isActive = false;
            this.mode = mode;
        }
        
        @Override
        public void run() {
            try {
                final Dataset ds = new Dataset(new File(new File("datasets"), dataset));
                isActive = true;
                File f = new File("runs");
                f.mkdirs();
                long time = System.currentTimeMillis();
                final Evaluate.EvaluationResults er;
                final AlignmentSet alignment;
                if(mode == ExecutionMode.EVALUATE) {
                    alignment = Main.execute(ds.left(), ds.right(),
                            config, listener);
                    if (alignment == null) {
                        return;
                    }
                    time = System.currentTimeMillis() - time;
                    Option<File> alignFile = ds.align();
                    if (alignFile.has()) {
                        listener.updateStatus(Stage.EVALUATION, "Evaluating");
                        AlignmentSet gold = Train.readAlignments(alignFile.get());
                        er = Evaluate.evaluate(alignment, gold, listener);
                    } else {
                        er = null;
                    }
                } else if(mode == ExecutionMode.TRAIN) {
                    Option<File> alignFile = ds.align();
                    if(!alignFile.has()) {
                        throw new IllegalArgumentException("Training was requested on run with no gold standard alignments");
                    }
                    Train.execute(ds.left(),
                            ds.right(), 
                            alignFile.get(), config, listener);
                    time = System.currentTimeMillis() - time;
                    er = null;
                    alignment = null;
                } else if(mode == ExecutionMode.CROSSFOLD) {
                    Option<File> alignFile = ds.align();
                    if(!alignFile.has()) {
                        throw new IllegalArgumentException("Cross-fold was requesetd on run with no gold standard alignments");
                    }
                    CrossFold.CrossFoldResult result = CrossFold.execute(
                            ds.left(),
                            ds.right(), 
                            alignFile.get(), 10, config, listener);
                    time = System.currentTimeMillis() - time;
                    er = result.results;
                    alignment = result.alignments;
                } else {
                    throw new RuntimeException("Unreachable");
                }
                listener.updateStatus(Stage.COMPLETED, "Completed");
                Run run = new Run(id, configName, dataset,
                        er == null ? -1.0 : er.precision(),
                        er == null ? -1.0 : er.recall(),
                        er == null ? -1.0 : er.fmeasure(),
                        er == null ? -2.0 : er.correlation, time,
                        mode == ExecutionMode.TRAIN);
                ManageServlet.completed.put(id, run);
                if(alignment != null) {
                    listener.saveAligment(run, alignment);
                }
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
