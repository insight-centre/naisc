package org.insightcentre.uld.naisc.main;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.uld.naisc.ConfigurationClass;
import org.insightcentre.uld.naisc.ConfigurationParameter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ConfigurationToMarkDown {

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            final OptionParser p = new OptionParser() {{
                nonOptions("The file to write the Markdown description of the configuration too");
            }};
            final OptionSet os;
            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            // Validate options
            final File f;
            if (os.nonOptionArguments().isEmpty()) {
                f = new File("CONFIGURATION.md");
            } else if (os.nonOptionArguments().size() != 1) {
                badOptions(p, "Wrong number of command line arguments");
                return;
            } else {
                f = new File(os.nonOptionArguments().get(0).toString());
            }
            try(PrintWriter out = new PrintWriter(f)) {
                writeSingleConfiguration(out);
            }
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    private static void writeSingleConfiguration(PrintWriter out) throws Exception {
        out.println("# Naisc Configuration Guidelines");
        out.println("");
        out.println("Naisc has a number of ways that it can be configured through the use of Json configuration files,");
        out.println("this document describes all the configuration parameters that are available for Naisc. This document ");
        out.println("is auto-generated based on the annotations in the codebase.\n" +
                "\n" +
                "The configuration file consists of the following sections:\n" +
                "\n" +
                "* `blocking`\n" +
                "* `lenses`\n" +
                "* `textFeatures`\n" +
                "* `graphFeatures`\n" +
                "* `scorers`\n" +
                "* `matchers`\n" +
                "* `rescaler`\n" +
                "* Other global properties\n" +
                "\n" +
                "Each of these is described in more detail in the following section. Every component is specified by an\n" +
                "object with the property `name` to indicate the component to be used.");
        out.println();
        out.println("## Blocking Strategy Configurations");
        out.println("\n" +
                "Blocking strategies occur in the `blocking` section of the configuration. There is only a single blocking \n" +
                "strategy so value of `blocking` is an object with a `name` property.\n" +
                "\n");
        for(Class config : Configuration.knownBlockingStrategies) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Lens Configurations");
        out.println("\n" +
                "Lens configuration is given in the `lenses` section of the configuration. There may be multiple lenses and as \n" +
                "such the `lenses` parameter takes an array of objects, where each object has a `name`.\n\n");
        for(Class config : Configuration.knownLenses) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Text Feature Configurations");
        out.println("\n" +
                "Text features are given in the `textFeatures` section of the configuration. There may be multiple text features\n" +
                "so  the `textFeatures` parameter takes an array of objects, where each object has a `name`. In addition, you may\n" +
                "provide a `tags` parameter to any text feature, which selects the lenses it may use.\n\n");
        for(Class config : Configuration.knownTextFeatures) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Graph Feature Configurations");
        out.println("\n" +
                "Graph features are given in the `graphFeatures` section of the configuration. There may be multiple graph features\n" +
                "so the `graphFeatures` parameter takes an array of objects, where each object has a `name`.\n\n");
        for(Class config : Configuration.knownGraphFeatures) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Scorer Configurations");
        out.println("\n" +
                "Scorers are given in the `scorers` section of the configuration. There may be multiple scorers (associated with\n" +
                "predicting different properties) so this parameter takes an array of objects, where each object has a `name`.\n\n");
        for(Class config : Configuration.knownScorers) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Matcher Configuration");
        out.println("\n" +
                "The matcher is given in the `matcher` section of the configuration. It should be a single object with a `name`.\n\n");
        for(Class config : Configuration.knownMatchers) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Rescaler Configuration (experimental)");
        out.println("\n" +
                "Rescalers are still experimental, currently you can only configure to use one of the following methods:\n\n" +
                "* `NoScaling`: Do not rescale the results of the scorer\n" +
                "* `MinMax`: Rescale the results of the scorer so that the highest prediction is 1 and the lowest is 0\n" +
                "* `Percentile`: Rescale so that the values correspond to the percentile of values that have this value. e.g., 0.5 means that score is exactly the mode of the dataset\n");

        out.println("## Other parameters");
        out.println("\n" +
                "The following further parameters are supported by Naisc:\n" +
                "" +
                "* `nThreads`: The maximum number of threads to use when aligning *(int > 0)*\n" +
                "* `includeFeatures`: The calculated features will be included in the output alignments (can make the alignment files very large!) *(boolean)*\n" +
                "* `ignorePreexisting`: If there are any links between the datasets already they will be discarded and Naisc will only infer new links *(boolean)*\n" +
                "* `noPrematching`: Do not attempt to find unambiguous links and use the full pipeline for every link inference *(boolean)*\n");
    }

    private static void writeSingleConfiguration(PrintWriter out, Class config) throws Exception {
        Class configClass = Class.forName(config.getName() + "$Configuration");
        ConfigurationClass cc = (ConfigurationClass)configClass.getAnnotation(ConfigurationClass.class);
        if(cc != null && cc.description() != null && !cc.description().equals("")) {
            out.println(cc.description());
        }
        if(cc != null && cc.name() != null && !cc.name().equals("")) {
            out.println("### " + cc.name());
        } else {
            out.println("### " + config.getSimpleName().replaceAll("([a-z])([A-Z])","$1 $2"));
        }
        out.println("");
        out.println("**Name:** `" + config.getName().substring("org.insightcentre.uld.naisc.".length()) + "`");
        if(configClass.getFields().length > 0) {
            out.println("");
            out.println("#### Configuration Parameters");
            out.println("");
            for(Field f : configClass.getFields()) {
                out.print("* `" + f.getName() + "`: ");
                ConfigurationParameter p = f.getAnnotation(ConfigurationParameter.class);
                if(p != null){
                    out.print(p.description());
                    if(p.defaultValue() != null && !p.defaultValue().equals("")) {
                        out.print(" (Default value: " + p.defaultValue() + ")");
                    }
                } else {
                    out.print("*No Description*");
                }
                if(f.getType().isEnum()) {
                    out.print(" *One of ");
                    boolean first = true;
                    for(String e : getEnumValues(f.getType())) {
                        if(first) {
                            first = false;
                        } else {
                            out.print("|");
                        }
                        out.print(e);
                    }
                    out.println("*");
                } else {
                    out.println(" *(" + f.getType().getSimpleName() + ")*");
                }
            }
        } else {
            out.println("No parameters");
        }
        out.println();
    }

    private static <E> String[] getEnumValues(Class<E> enumClass)
            throws NoSuchFieldException, IllegalAccessException {
        Field f = enumClass.getDeclaredField("$VALUES");
        System.out.println(f);
        System.out.println(Modifier.toString(f.getModifiers()));
        f.setAccessible(true);
        E[] o = (E[])f.get(null);
        String[] names = new String[o.length];
        for(int i = 0; i < o.length; i++) {
            names[i] = ((Enum)o[i]).name();
        }
        return names;
    }
}
