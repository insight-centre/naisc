package org.insightcentre.uld.naisc.scorer;

/**
 * Indicates that the model is not trained for the problem
 * @author John McCrae
 */
public class ModelNotTrainedException extends Exception {

    public ModelNotTrainedException() {
    }

    public ModelNotTrainedException(String s) {
        super(s);
    }

    public ModelNotTrainedException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ModelNotTrainedException(Throwable throwable) {
        super(throwable);
    }
}
