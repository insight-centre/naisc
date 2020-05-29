package org.insightcentre.uld.naisc.meas.execution;

import java.io.File;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.main.CrossFold;
import org.insightcentre.uld.naisc.main.Evaluate;
import org.insightcentre.uld.naisc.main.ExecuteListener;
import org.insightcentre.uld.naisc.main.Main;
import org.insightcentre.uld.naisc.main.Train;
import org.insightcentre.uld.naisc.EvaluationSet;
import org.insightcentre.uld.naisc.meas.ManageServlet;
import org.insightcentre.uld.naisc.meas.Meas;
import org.insightcentre.uld.naisc.meas.MeasDatasetLoader;
import org.insightcentre.uld.naisc.scorer.ModelNotTrainedException;
import org.insightcentre.uld.naisc.util.Option;

/**
 * The task that the thread executing the system will run
 * 
 * @author John McCrae
 */
public class ExecutionTask implements Runnable {

    private final Configuration config;
    public final String dataset;
    public final String id;
    public final String configName;
    public final Execution listener;
    public boolean isActive;
    private final ExecutionMode mode;
    private final String requestURL;
    private final Option<AlignmentSet> userAligns;
    private final int nFolds;
    private final CrossFold.FoldDirection direction;

    public ExecutionTask(Configuration config, String configName, String dataset, String id, ExecutionMode mode,
                         String requestURL, Option<AlignmentSet> userAligns, int nFolds, CrossFold.FoldDirection direction) {
        this.config = config;
        this.dataset = dataset;
        this.id = id;
        this.configName = configName;
        this.listener = new Execution(id);
        this.isActive = false;
        this.mode = mode;
        this.requestURL = requestURL;
        this.userAligns = userAligns;
        this.nFolds = nFolds;
        this.direction = direction;
    }

    @Override
    public void run() {
        try (final MeasDatasetLoader loader = new MeasDatasetLoader(requestURL)) {
            final EvaluationSet ds = new EvaluationSet(new File(new File("datasets"), dataset));
            isActive = true;
            File f = new File("runs");
            f.mkdirs();
            File f2 = new File(f, id + ".db");
            if(f2.exists())
                f2.delete();
            long time = System.currentTimeMillis();
            final Evaluate.EvaluationResults er;
            final AlignmentSet alignment;
            if (mode == ExecutionMode.EVALUATE) {
                if(userAligns.has()) {
                    // We are re-running so we need to clear the tables for the new alignments
                    listener.clearAlignments(); 
                }
                if(ds.align().has() && ds.trainAlign().has()) {
                    alignment = Main.executeLimitedToGold(id, ds.left(), ds.right(), ds.align().get(), config, userAligns, listener, loader);
                } else {
                    alignment = Main.execute2(id, ds.left(), ds.right(), config, userAligns, listener, loader);
                }
                if (alignment == null) {
                    return;
                }
                time = System.currentTimeMillis() - time;
                Option<File> alignFile = ds.align();
                if (alignFile.has()) {
                    listener.updateStatus(ExecuteListener.Stage.EVALUATION, "Evaluating");
                    AlignmentSet gold = Train.readAlignments(alignFile.get(), "left", "right");
                    er = Evaluate.evaluate(alignment, gold, listener, ds.trainAlign().has());
                } else {
                    er = null;
                }
            } else if (mode == ExecutionMode.TRAIN) {
                if (userAligns.has()) {
                    Train.execute(id, ds.left(), ds.right(), userAligns.get(), 5.0, config, listener, loader, null);
                    time = System.currentTimeMillis() - time;
                    er = null;
                    alignment = null;
                } else {
                    Option<File> alignFile = ds.align();
                    if (!alignFile.has()) {
                        throw new IllegalArgumentException("Training was requested on run with no gold standard alignments");
                    }
                    Train.execute(id, ds.left(), ds.right(), alignFile.get(), 5.0, config, listener, loader, null);
                    time = System.currentTimeMillis() - time;
                    er = null;
                    alignment = null;
                }
            } else if (mode == ExecutionMode.CROSSFOLD) {
                Option<File> alignFile = ds.align();
                if (!alignFile.has()) {
                    throw new IllegalArgumentException("Cross-fold was requesetd on run with no gold standard alignments");
                }
                CrossFold.CrossFoldResult result = CrossFold.execute(id, ds.left(), ds.right(), alignFile.get(), nFolds, direction, 5.0, config, listener, loader);
                time = System.currentTimeMillis() - time;
                er = result.results;
                alignment = result.alignments;
            } else {
                throw new RuntimeException("Unreachable");
            }
            listener.updateStatus(ExecuteListener.Stage.COMPLETED, "Completed");
            Meas.Run run = new Meas.Run(id, configName, dataset, er == null ? -1.0 : er.precision(), er == null ? -1.0 : er.recall(), er == null ? -1.0 : er.fmeasure(), er == null ? -2.0 : er.correlation, time, mode == ExecutionMode.TRAIN);
            ManageServlet.completed.put(id, run);
            if (alignment != null) {
                listener.saveAlignment(run, alignment, loader.fromFile(ds.left(), "left"), loader.fromFile(ds.right(), "right"));
            }
            Meas.data.runs.add(run);
        } catch (ModelNotTrainedException x) {
            x.printStackTrace();
            listener.response.stage = NaiscListener.Stage.FAILED;
            listener.response.lastMessage = "Model has not been trained or the trained model is incompatible with this dataset!";
        } catch (Exception x) {
            x.printStackTrace();
            listener.response.stage = ExecuteListener.Stage.FAILED;
            listener.response.lastMessage = x.getClass().getSimpleName() + ": " + x.getMessage();
        } finally {
            isActive = false;
        }
    }

}
