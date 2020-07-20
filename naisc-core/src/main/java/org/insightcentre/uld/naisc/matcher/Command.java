package org.insightcentre.uld.naisc.matcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.main.Configs;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.main.ExecuteListener;
import org.insightcentre.uld.naisc.util.ExternalCommandException;

/**
 * Use an external command for matching. The command will get as input the
 * problem as tab-seperated values consisting of `entity1`, `property`,
 * `entity2` and `probability`
 *
 * Input:  <code>
 * http://www.example.com/uri1  http://www.w3.org/2004/02/skos/core#exactMatch  http://www.example.com/uri2 0.5
 * http://www.example.com/uri3  http://www.w3.org/2004/02/skos/core#exactMatch  http://www.example.com/uri4 0.7
 * http://www.example.com/uri5  http://www.w3.org/2004/02/skos/core#exactMatch  http://www.example.com/uri6 Infinity
 * </code>
 *
 * The command is expected to print only those lines that are in the final
 * matching. A probability of Infinity means that the match must be included
 *
 * @author John McCrae
 */
public class Command implements MatcherFactory {

    @Override
    public String id() {
        return "command";
    }

    @Override
    public Matcher makeMatcher(Map<String, Object> params) {
        Configuration config = Configs.loadConfig(Configuration.class, params);
        if (config.command == null) {
            throw new ConfigurationException("Command cannot be null");
        }
        return new CommandImpl(config.command);
    }

    /**
     * The configuration of the external command based matcher
     */
    public static class Configuration {

        /**
         * The command to run.
         */
        @ConfigurationParameter(description = "The command to run")
        public String command;
    }

    private static class CommandImpl implements Matcher {

        private final String command;

        public CommandImpl(String command) {
            this.command = command;
        }


        @Override
        public AlignmentSet alignWith(AlignmentSet matches, AlignmentSet partialMatches, ExecuteListener listener) {

            Runtime rt = Runtime.getRuntime();
            try {
                Process pr = rt.exec(command);
                BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                //BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                final AlignmentSet result = new AlignmentSet();
                final Model model = ModelFactory.createDefaultModel();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try (PrintWriter out = new PrintWriter(pr.getOutputStream())) {
                            for (Alignment as : matches) {
                                if(partialMatches.contains(as)) {
                                    out.println(as.entity1 + "\t" + as.property + "\t" + as.entity2 + "\tInfinity");
                                    
                                } else {
                                    out.println(as.entity1 + "\t" + as.property + "\t" + as.entity2 + "\t" + as.probability);
                                }
                            }
                            out.flush();
                        }
                    }
                }).start();
                String line = in.readLine();
                while (line != null) {
                    String[] elems = line.split("\t");
                    if (elems.length != 4) {
                        throw new RuntimeException("Bad result from matcher: " + line);
                    }
                    result.add(new Alignment(new URIRes(elems[0], "left"),
                            new URIRes(elems[2], "right"), Double.parseDouble(elems[3]),
                            elems[1], null));
                    line = in.readLine();
                }

                /*String eline = err.readLine();
                while (eline != null) {
                    listener.updateStatus(ExecuteListener.Stage.MATCHING, eline);
                }*/
                return result;
            } catch (IOException x) {
                throw new ExternalCommandException(x);
            }
        }

    }

}
