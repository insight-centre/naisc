package org.insightcentre.uld.naisc.blocking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.main.Configs;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * The command version of the blocking strategy
 *
 * @author John McCrae
 */
public class Command implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> analysis, NaiscListener listener) {
        Configuration config = Configs.loadConfig(Configuration.class, params);
        if(config.command == null) {
            throw new ConfigurationException("Command cannot be null");
        }
        return new CommandImpl(config.command);
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

        private final String command;

        public CommandImpl(String command) {
            this.command = command;

        }

        @Override
        public Collection<Blocking> block(Dataset left, Dataset right, NaiscListener log) {
            URL leftSparql = left.asEndpoint().getOrExcept(new RuntimeException("Cannot run blocking command with SPARQL endpoint"));
            URL rightSparql = right.asEndpoint().getOrExcept(new RuntimeException("Cannot run blocking command with SPARQL endpoint"));
            return new AbstractCollection<Blocking> () {
                @Override
                public Iterator<Blocking> iterator() {
                    return new CommandRunner(command, leftSparql, rightSparql, left.id(), right.id());
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    private static class CommandRunner implements Iterator<Blocking> {

        Process pr;
        PrintWriter out;
        BufferedReader in;
        String line = null;
        final Model model;
        final String leftId, rightId;

        public CommandRunner(String command, URL leftSparql, URL rightSparql, String leftId, String rightId) {
            Runtime rt = Runtime.getRuntime();
            try {
                String co = command.replace("$SPARQL_LEFT", leftSparql.toString()).
                        replace("$SPARQL_RIGHT", rightSparql.toString());
                pr = rt.exec(co);
                out = new PrintWriter(pr.getOutputStream());
                in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                line = in.readLine();
                if (line == null) {
                    String eline = err.readLine();
                    while (eline != null) {
                        eline = err.readLine();
                    }
                    throw new RuntimeException("Failed to start command");
                }
                model = ModelFactory.createDefaultModel();
                this.leftId = leftId;
                this.rightId = rightId;
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public boolean hasNext() {
            return line != null;
        }

        @Override
        public Blocking next() {
            String[] elems = line.split("\t");
            if (elems.length != 2) {
                throw new RuntimeException("Bad output from commmand. Must be two URIs separated by a tab");
            }
            Pair<Resource, Resource> p = new Pair<>(
                    model.createResource(elems[0]),
                    model.createResource(elems[1])
            );
            try {
                line = in.readLine();
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
            return new Blocking(p._1, p._2, leftId, rightId);
        }

    }
}
