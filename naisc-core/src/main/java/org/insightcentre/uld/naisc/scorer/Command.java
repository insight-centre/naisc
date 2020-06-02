package org.insightcentre.uld.naisc.scorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.FeatureSet;
import org.insightcentre.uld.naisc.FeatureSetWithScore;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.ScoreResult;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.ScorerFactory;
import org.insightcentre.uld.naisc.ScorerTrainer;
import org.insightcentre.uld.naisc.main.Configs;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.ExternalCommandException;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * Use an external command as the scorer of matches. The command will be fed the
 * datapoints one line at a time as a Json array and should print a probability
 * between 0 and 1 on each corresponding line. For example
 *
 * Input:  <code>
 * [0.1,0.3,0.7]
 * [0.6,0.2,0.8]
 * </code>
 *
 * Output:  <code>
 * 0.9
 * 0.2
 * </code>
 *
 * The trainer is fed the same input but with the probability appended after a tab.
 * There is no expected output. Instead the `trainCommand` and `command` should
 * both refer to the same saved model file.
 *
 * Input:  <code>
 * [0.1,0.3,0.7]    0.9
 * [0.6,0.2,0.8]    0.2
 * </code>
 *
 * @author John McCrae
 */
public class Command implements ScorerFactory {

    @Override
    public String id() {
        return "command";
    }

    @Override
    public Scorer makeScorer(Map<String, Object> params, File modelPath) {
        Configuration config = Configs.loadConfig(Configuration.class, params);
        if (config.command == null) {
            throw new ConfigurationException("Command cannot be null");
        }
        return new CommandImpl(modelPath == null ? config.command : config.command.replace("$MODEL_PATH", modelPath.getAbsolutePath()), config.property);
    }

    @Override
    public Option<ScorerTrainer> makeTrainer(Map<String, Object> params, String property, File modelPath) {
        Configuration config = Configs.loadConfig(Configuration.class, params);
        if (property.equals(config.property)) {
            if (config.trainCommand == null || config.trainCommand.equals("")) {
                return new None<>();
            }
            if (config.command == null) {
                throw new ConfigurationException("Command cannot be null");
            }
            return new Some<>(new CommandTrainerImpl(modelPath == null ? config.trainCommand : config.trainCommand.replace("$MODEL_PATH", modelPath.getAbsolutePath()), 
                    modelPath == null ? config.command : config.command.replace("$MODEL_PATH", modelPath.getAbsolutePath()), config.property));
        } else {
            return new None<>();
        }
    }

    /**
     * The configuration for external command scorers
     */
    public static class Configuration {

        /**
         * The command to run.
         */
        @ConfigurationParameter(description = "The command to run. Use $MODEL_PATH to indicate the path to the model.")
        public String command;

        /**
         * The command to run the trainer or null for no trainer.
         */
        @ConfigurationParameter(description = "The command to run the trainer. Use $MODEL_PATH to indicate the path to the model.")
        public String trainCommand;

        /**
         * The property to output.
         */
        @ConfigurationParameter(description = "The property to output")
        public String property;
    }

    private static class CommandImpl implements Scorer {

        private final String relation;
        private final ThreadLocal<PrintWriter> out;
        private final ThreadLocal<BufferedReader> in;
        private final ObjectMapper mapper = new ObjectMapper();

        public CommandImpl(String command, String relation) {
            this.relation = relation;
            final ThreadLocal<Process> pr = new ThreadLocal<Process>() {
                @Override
                protected Process initialValue() {
                    try {
                        final Runtime rt = Runtime.getRuntime();
                        return rt.exec(command);
                    } catch (IOException x) {
                        throw new ExternalCommandException(x);
                    }
                }
            };
            out = new ThreadLocal<PrintWriter>() {
                @Override
                protected PrintWriter initialValue() {
                    return new PrintWriter(pr.get().getOutputStream());
                }
            };
            in = new ThreadLocal<BufferedReader>() {
                @Override
                protected BufferedReader initialValue() {
                    return new BufferedReader(new InputStreamReader(pr.get().getInputStream()));
                }
            };
        }

        @Override
        public List<ScoreResult> similarity(FeatureSet features, NaiscListener log) {
            try {
                out.get().println(mapper.writeValueAsString(features.values));
                out.get().flush();
                String line = in.get().readLine();
                return Collections.singletonList(new ScoreResult(Double.parseDouble(line.trim()), relation));
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void close() throws IOException {
            in.get().close();
            out.get().close();
        }

    }

    private static class CommandTrainerImpl implements ScorerTrainer {

        private final String trainCommand, command, relation;
        private final ObjectMapper mapper = new ObjectMapper();

        public CommandTrainerImpl(String trainCommand, String command, String relation) {
            this.trainCommand = trainCommand;
            this.command = command;
            this.relation = relation;
        }

        @Override
        public Scorer train(List<FeatureSetWithScore> dataset, NaiscListener log) {

            Runtime rt = Runtime.getRuntime();
            try {
                Process pr = rt.exec(trainCommand);
                PrintWriter out = new PrintWriter(pr.getOutputStream());
                for (FeatureSetWithScore fss : dataset) {
                    out.println(mapper.writeValueAsString(fss.values) + "\t" + fss.score);
                }
                return new CommandImpl(command, relation);
            } catch (IOException x) {
                throw new ExternalCommandException(x);
            }
        }

        @Override
        public String property() {
            return relation;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void save(Scorer scorer) throws IOException {
            
        }
    }
}
