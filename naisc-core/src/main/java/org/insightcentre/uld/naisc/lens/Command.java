package org.insightcentre.uld.naisc.lens;

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
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.LensFactory;
import org.insightcentre.uld.naisc.main.Configs;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.ExternalCommandException;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * Run an external tool for lens extraction. The command should expect to receives
 * pairs of URLs for each pair and return a Json encoded `LangStringPair` object
 * or a blank line for no results
 * For example
 * 
  * Input:
 * <code>
 * http://example.com/url1  http://example.com/url2
 * http://example.com/url3  http://example.com/url4
 * http://example.com/url5  http://example.com/url6
 * </code>
 * 
 * Output: 
 * <code>
 * {"_1":"url1","_2":"url2","lang1":"en","lang2":"en"}
 * 
 * {"_1":"label","_2":"label","lang1":"und","lang2":"de"}
 * </code>
 * 
 * @author John McCrae
 */
public class Command implements LensFactory {

    @Override
    public Lens makeLens(String tag, Dataset dataset, Map<String, Object> params) {
        Configuration config = Configs.loadConfig(Configuration.class, params);
        if(config.command == null) {
            throw new ConfigurationException("Command cannot be null");
        }
        if(config.id == null) {
            throw new ConfigurationException("Identifier cannot be null");
        }
        if(config.command.contains("$SPARQL")) {
            URL sparql = dataset.asEndpoint().getOrExcept(new RuntimeException("Command requires a SPARQL endpoint"));
            config.command = config.command.replace("$SPARQL", sparql.toString());
        }
        return new CommandImpl(config.id, tag, config.command);
    }

    /**
     * The configuration for the external command lens
     */
    public static class Configuration {

        /**
         * The command to run, the SPARQL endpoint for the data will be provided
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

    private static class CommandImpl implements Lens {
        private final String id, tag;
        Process pr;
        PrintWriter out;
        BufferedReader in;
        private final ObjectMapper mapper = new ObjectMapper();

        public CommandImpl(String id, String tag, String command) {
            this.id = id;
            this.tag = tag;
            Runtime rt = Runtime.getRuntime();
            try {
                pr = rt.exec(command);
                out = new PrintWriter(pr.getOutputStream());
                in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            } catch (IOException x) {
                throw new ExternalCommandException(x);
            }
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Option<LangStringPair> extract(Resource entity1, Resource entity2) {
            out.println(entity1.getURI() + "\t" + entity2.getURI());
            out.flush();
            try {
                String line = in.readLine();
            
                if (line == null) {
                    BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    String eline = err.readLine();
                    while (eline != null) {
                        System.err.println(eline);
                        eline = err.readLine();
                    }
                    throw new RuntimeException("Command failed");
                }
                if(line.equals("")) {
                    return new None<>();
                } else {
                    return new Some<>(mapper.readValue(line, LangStringPair.class));
                }
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public String tag() {
            return tag;
        }

    }
}
