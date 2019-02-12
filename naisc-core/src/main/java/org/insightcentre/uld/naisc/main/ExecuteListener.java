package org.insightcentre.uld.naisc.main;

import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.util.LangStringPair;

/**
 * A monitor of the execution status
 * @author John McCrae
 */
public interface ExecuteListener {
    public static enum Stage {
        INITIALIZING,
        BLOCKING,
        SCORING,
        MATCHING,
        TRAINING,
        FINALIZING,
        FAILED,
        COMPLETED,
        EVALUATION
    }
    
    /**
     * Update the status
     * @param stage The current stage
     * @param message The message about the most recent action
     */
    public void updateStatus(Stage stage, String message);
    
    /**
     * Used to notify the execution about the result of a lens
     * @param id1 The left entity
     * @param id2 The right entity
     * @param lensId The id of the lens
     * @param res The lens extracted value
     */
    public void addLensResult(Resource id1, Resource id2, String lensId, LangStringPair res);
    
    /**
     * Notify of a blocking result. This is used to provide alternatives to the
     * user in the interface
     * @param res1 The left resource
     * @param res2 The right resource
     */
    default void addBlock(Resource res1, Resource res2) { }
}
