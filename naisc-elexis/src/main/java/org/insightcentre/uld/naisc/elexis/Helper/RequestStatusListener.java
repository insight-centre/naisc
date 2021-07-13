package org.insightcentre.uld.naisc.elexis.Helper;

import org.insightcentre.uld.naisc.LensResult;
import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.main.ExecuteListener;

/**
 * Defines a class Implementing ExecuteListener to track the status of the linking request
 *
 * @author Suruchi Gupta
 */
public class RequestStatusListener implements ExecuteListener {
    private Stage stage;
    private String message;
    private Level level;

    public Stage getStage() {
        return stage;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void updateStatus(Stage stage, String message) {
        this.stage = stage;
        this.level = null;
        this.message = message;
    }

    @Override
    public void addLensResult(URIRes id1, URIRes id2, String lensId, LensResult res) {

    }

    @Override
    public void message(Stage stage, Level level, String message) {
        this.stage = stage;
        this.level = level;
        this.message = message;
    }
}
