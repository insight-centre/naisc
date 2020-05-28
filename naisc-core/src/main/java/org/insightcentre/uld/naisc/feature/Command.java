package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.main.Configs;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.ExternalCommandException;
import org.insightcentre.uld.naisc.util.LangStringPair;

/**
 * Use an external command for text feature extraction. The input to this will
 * be a single line with a JSON object of the form:
 *
 * <code>
 * {"lang1":"en","lang2":"en","_1":"Label 1","_2":"Label 2"}
 * </code>
 *
 * The output should be as follows. The first line should be an array of strings
 * giving the feature names, and every value after should be an array of number
 * of the same length. There should be precisely one response for each line of
 * input , e.g.,
 *
 * <code>
 * ["feature1","feature2"]
 * [0.2,0.3]
 * [0.4,0.6]
 * </code>
 *
 * @author John McCrae
 */
public class Command implements TextFeatureFactory {

    @Override
    public TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params) {
        Configuration config = Configs.loadConfig(Configuration.class, params);
        if (config.command == null) {
            throw new ConfigurationException("Command not set");
        }
        if (config.id == null) {
            throw new ConfigurationException("Command does not have an ID");
        }
        return new CommandImpl(config.id, tags, config.command);
    }

    /**
     * The class for configuring the command
     */
    public static class Configuration {

        /**
         * The command to run
         */
        @ConfigurationParameter(description = "The command to run")
        public String command;

        /**
         * The identifier
         */
        @ConfigurationParameter(description = "The identifier")
        public String id;
    }

    private static class CommandImpl implements TextFeature {

        private final String id;
        private final Set<String> tags;
        private String[] features;
        private final ThreadLocal<Process> pr;
        private final ThreadLocal<PrintWriter> out;
        private final ThreadLocal<BufferedReader> in;
        private final ObjectMapper mapper = new ObjectMapper();

        public CommandImpl(String id, Set<String> tags, String command) {
            this.id = id;
            this.tags = tags;
            pr = new ThreadLocal<Process>() {
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
                    BufferedReader input = new BufferedReader(new InputStreamReader(pr.get().getInputStream()));
                    BufferedReader err = new BufferedReader(new InputStreamReader(pr.get().getErrorStream()));
                    try {
                        String line = input.readLine();
                        if (line == null) {
                            String eline = err.readLine();
                            while (eline != null) {
                                eline = err.readLine();
                            }
                            throw new RuntimeException("Command failed to start");
                        }
                        features = mapper.readValue(line, mapper.getTypeFactory().constructArrayType(String.class));
                    } catch (IOException x) {
                        throw new ExternalCommandException(x);
                    }
                    return input;
                }
            };
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Feature[] extractFeatures(LensResult facet, NaiscListener log) {
            try {
                out.get().println(mapper.writeValueAsString(facet));
                return mapper.readValue(in.get().readLine(), Feature[].class);
            } catch (IOException x) {
                throw new RuntimeException();
            }
        }

        @Override
        public String[] getFeatureNames() {
            in.get();
            return features;
        }

        @Override
        public Set<String> tags() {
            return tags;
        }

        @Override
        public void close() throws IOException {
            pr.get().destroy();
        }

    }
}
