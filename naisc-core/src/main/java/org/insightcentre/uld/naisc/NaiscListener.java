package org.insightcentre.uld.naisc;

/**
 * Listens to messages from individual components that can be shown to the user
 * 
 * @author John McCrae
 */
public interface NaiscListener {
    /**
     * Indicates the stage of the execution
     */
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
     * Indicates the level of a message
     */
    public static enum Level {
        /**
         * The process cannot continue and the extraction will fail
         */        
        CRITICAL,
        /**
         * The process can continue but it this may affect the success of the run
         */
        WARNING,
        /**
         * The process has some information from the normal execution
         */
        INFO        
    }
    
    /**
     * Send a message
     * @param stage The stage of the alignment
     * @param level The level of the message
     * @param message The message
     */
    void message(Stage stage, Level level, String message);
    
    /**
     * Default messenger ignores all messages
     */
    public static final NaiscListener DEFAULT = new NaiscListener() {
        @Override
        public void message(Stage stage, Level level, String message) {
        }
    };
}
