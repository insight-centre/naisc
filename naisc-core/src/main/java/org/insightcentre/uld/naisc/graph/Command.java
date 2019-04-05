package org.insightcentre.uld.naisc.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Map;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.GraphFeatureFactory;
import org.insightcentre.uld.naisc.main.Configs;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.ExternalCommandException;

/**
 * Graph feature extraction using an external command. The command will receive the 
 * two entities to produce features for as tab separated strings. It should return
 * as following: Firstly it should print as a Json string array the name of each feature
 * and then for each line of input the result as a Json double array, e.g.,
 * 
 * Input:
 * <code>
 * http://example.com/url1  http://example.com/url2
 * http://example.com/url3  http://example.com/url4
 * </code>
 * 
 * Output: 
 * <code>
 * ["foo","bar"]
 * [0,1]
 * [0.2,0.5]
 * </code>
 *
 * @author John McCrae
 */
public class Command implements GraphFeatureFactory {

    @Override
    public GraphFeature makeFeature(Dataset sparqlData, Map<String, Object> params) {
        Configuration config = Configs.loadConfig(Configuration.class, params);
        if(config.command == null) {
            throw new ConfigurationException("Command cannot be null");
        }
        if(config.id == null) {
            throw new ConfigurationException("Identifier cannot be null");
        }
        if(config.command.contains("$SPARQL")) {
            URL sparql = sparqlData.asEndpoint().getOrExcept(new RuntimeException("Command requires a SPARQL endpoint"));
            config.command = config.command.replace("$SPARQL", sparql.toString());
        }
        return new CommandImpl(config.id, config.command);
    }

    /**
     * Configuration of the graph feature extraction command
     */
    public static class Configuration {

        /**
         * The command to run, the sparql endpoint for the data will be provided
         * as $SPARQL
         */
        @ConfigurationParameter(description = "The command to run, the sparql endpoint for the data will be provided as $SPARQL")
        public String command;

        /**
         * The identifier of this feature extractor
         */
        @ConfigurationParameter(description = "The identifier of this feature extractor")
        public String id;
    }

    private static class CommandImpl implements GraphFeature {

        private final String id;
        private final ThreadLocal<Process> pr;
        private final ThreadLocal<PrintWriter> out;
        private final ThreadLocal<BufferedReader> in, err;
        private final ObjectMapper mapper = new ObjectMapper();
        private String[] features;

        public CommandImpl(String id, String command) {
            this.id = id;
            
            
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
            err = new ThreadLocal<BufferedReader>() {
                @Override
                protected BufferedReader initialValue() {
                    return new BufferedReader(new InputStreamReader(pr.get().getErrorStream()));
                }
            };
        
            in = new ThreadLocal<BufferedReader>() {
                @Override
                protected BufferedReader initialValue() {
                    BufferedReader input = new BufferedReader(new InputStreamReader(pr.get().getInputStream()));
                    try {
                        String line = input.readLine();
                        if (line == null) {
                            String eline = err.get().readLine();
                            while (eline != null) {
                                System.err.println(eline);
                                eline = err.get().readLine();
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
        public double[] extractFeatures(Resource entity1, Resource entity2) {

            try {
                out.get().println(entity1.getURI() + "\t" + entity2.getURI());
                out.get().flush();
                return mapper.readValue(in.get().readLine(), double[].class);
            } catch (IOException x) {
                throw new RuntimeException();
            }
        }

        @Override
        public String[] getFeatureNames() {
            return features;
        }

    }

}
