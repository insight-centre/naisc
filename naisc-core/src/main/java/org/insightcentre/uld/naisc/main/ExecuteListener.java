package org.insightcentre.uld.naisc.main;

import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.LensResult;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.util.LangStringPair;

/**
 * A monitor of the execution status
 * @author John McCrae
 */
public interface ExecuteListener extends NaiscListener {

    
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
    public void addLensResult(URIRes id1, URIRes id2, String lensId, LensResult res);
    
    /**
     * Notify of a blocking result. This is used to provide alternatives to the
     * user in the interface
     * @param res1 The left resource
     * @param res2 The right resource
     */
    default void addBlock(URIRes res1, URIRes res2) { }
}
