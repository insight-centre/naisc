package org.insightcentre.uld.naisc.blocking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.BlockingStrategyFactory;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * The command version of the blocking strategy
 *
 * @author John McCrae
 */
public class Command implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Configuration for the blocking strategy command
     */
    public static class Configuration {

        /**
         * The command to run; it should have to slots $SPARQL_LEFT and
         * $SPARQL_RIGHT for the URL of the left and right SPARQL endpoint
         */
        @ConfigurationParameter(description = "The command to run it should have to slots $SPARQL_LEFT and $SPARQL_RIGHT for the"
                + " URL of the left and right SPARQL endpoint")
        public String command;
    }

    private static class CommandImpl implements BlockingStrategy {

        Process pr;
        PrintWriter out;
        BufferedReader in;
        private final String command;
        
        public CommandImpl(String command) {
            this.command = command;
            
           /* Runtime rt = Runtime.getRuntime();
            try {
                pr = rt.exec(command);
                out = new PrintWriter(pr.getOutputStream());
                in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                String line = in.readLine();
                if (line == null) {
                    String eline = err.readLine();
                    while (eline != null) {
                        System.err.println(eline);
                        eline = err.readLine();
                    }
                    throw new RuntimeException("Command failed to start");
                }
                features = mapper.readValue(line, mapper.getTypeFactory().constructArrayType(String.class));
            } catch (IOException x) {
                throw new RuntimeException(x);
            }*/
        }
        
        @Override
        public Iterable<Pair<Resource, Resource>> block(Dataset left, Dataset right) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
