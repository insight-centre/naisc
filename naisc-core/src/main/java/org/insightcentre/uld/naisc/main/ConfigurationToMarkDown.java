package org.insightcentre.uld.naisc.main;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
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
        out.println("is auto-generated based on the annotations in the codebase.");
        out.println();
        out.println("## Blocking Strategy Configurations");
        out.println("");
        for(Class config : Configuration.knownBlockingStrategies) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Lens Configurations");
        out.println("");
        for(Class config : Configuration.knownLenses) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Text Feature Configurations");
        out.println("");
        for(Class config : Configuration.knownTextFeatures) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Graph Feature Configurations");
        out.println("");
        for(Class config : Configuration.knownGraphFeatures) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Scorer Configurations");
        out.println();
        for(Class config : Configuration.knownScorers) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Matcher Configuration");
        out.println();
        for(Class config : Configuration.knownMatchers) {
            writeSingleConfiguration(out, config);
        }

        out.println("");
        out.println("## Rescaler Configuration (experimental)");
        out.println("");


    }

    private static void writeSingleConfiguration(PrintWriter out, Class config) throws Exception {
        out.println("### " + config.getSimpleName().replaceAll("([a-z])([A-Z])","$1 $2"));
        out.println("");
        out.println("**Name:** " + config.getName().substring("org.insightcentre.uld.naisc.".length()));
        Class configClass = Class.forName(config.getName() + "$Configuration");
        if(configClass != null || configClass.getFields().length > 0) {
            out.println("");
            out.println("#### Configuration Parameters");
            out.println("");
            for(Field f : configClass.getFields()) {
                out.print("* **" + f.getName() + "**:");
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
                    out.println(" *<" + f.getType().getSimpleName() + ">*");
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
