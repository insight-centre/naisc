package org.insightcentre.uld.naisc.main;

import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.util.LangStringPair;

/**
 * Standard Execute Listeners
 * @author John McCrae
 */
public class ExecuteListeners {
    private ExecuteListeners() {}
    
    public static ExecuteListener STDERR = new StdErrMonitor();
    public static ExecuteListener NONE = new NoMonitor();
    
    private static class StdErrMonitor implements ExecuteListener {

        @Override
        public void updateStatus(ExecuteListener.Stage stage, String message) {
            System.err.println("[" + stage + "] " + message);
        }

        @Override
        public void addLensResult(Resource id1, Resource id2, String lensId, LangStringPair res) {
        }

        @Override
        public void message(Stage stage, Level level, String message) {
            System.err.println("[" + stage + "] " + level + ": " + message);
        }

        
    }

    private static class NoMonitor implements ExecuteListener {

        @Override
        public void updateStatus(ExecuteListener.Stage stage, String message) {
        }

        @Override
        public void addLensResult(Resource id1, Resource id2, String lensId, LangStringPair res) {
        }

        @Override
        public void message(Stage stage, Level level, String message) {
        }

    }
}
