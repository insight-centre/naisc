package org.insightcentre.uld.naisc.meas;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_OK;
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
        if(path == null) return;
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
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.err.println(req.getPathInfo());
        String path = req.getPathInfo();
        if(path.startsWith("/save/")) {
            String id = path.substring(6);
            if(id.matches(VALID_ID)) {
                SaveData data = mapper.readValue(req.getReader(), SaveData.class);
                Run oldRun = completed.get(data.identifier);
                if(oldRun == null) {
                    oldRun = Execution.loadRun(id);
                }
                if(oldRun != null) {
                    Run run = new Meas.Run(data.identifier, oldRun.configName, oldRun.datasetName, data.precision, data.recall, data.fmeasure, -2, oldRun.time);
                    completed.put(data.identifier, run);
                    Meas.updateRun(data.identifier, run);
                    new Execution(data.identifier).updateAlignment(run, data.results);
                    resp.setStatus(SC_OK);
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not locate ID");
                }
            }
        } else if(path.startsWith("/rerun/")) {
            throw new UnsupportedOperationException("TODO");
        } else if(path.startsWith("/retrain/")) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    public static class SaveData {
        
        public String identifier;
        public double precision, recall, fmeasure;
        public List<Meas.RunResultRow> results;
    }
    
}
