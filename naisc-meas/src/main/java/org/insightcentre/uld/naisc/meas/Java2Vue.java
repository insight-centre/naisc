package org.insightcentre.uld.naisc.meas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.main.Configuration;

/**
 * Convert a Java configuration to a Vue component
 *
 * @author John McCrae
 */
public class Java2Vue {

    public static String java2vue(Class configClass) {
        StringBuilder sb = new StringBuilder();
        _java2vue(configClass, sb, "config");
        return sb.toString();
    }

    private static void _java2vue(Class configClass, StringBuilder sb, String path) {
        ObjectMapper mapper = new ObjectMapper();
        for (Field f : configClass.getFields()) {
            ConfigurationParameter param = f.getAnnotation(ConfigurationParameter.class);
            String description = param == null ? "" : param.description();
            Object defaultValue = annoDefaultValue(param, mapper);
            if (Modifier.isStatic(f.getModifiers())) {

            } else if (f.getType().equals(String.class)) {
                sb.append("<div class=\"form-group\">\n");
                sb.append("  <label for=\"").append(toVar(path)).append(f.getName()).append("\">").append(deCamelCase(f.getName())).append("</label>\n");
                sb.append("  <input type=\"text\" class=\"form-control\" id=\"").
                        append(toVar(path)).append("__").append(f.getName()).append("\" v-model=\"").
                        append(path).append("__").append(f.getName()).append("\">\n");
                if (description.length() > 0) {
                    sb.append("  <small class=\"form-text text-muted\">").append(description).append("</small>\n");
                }
                sb.append("</div>\n");
            } else if (f.getType().equals(int.class)) {
                sb.append("<div class=\"form-group\">\n");
                sb.append("  <label for=\"").append(toVar(path)).append(f.getName()).append("\">").append(deCamelCase(f.getName())).append("</label>\n");
                sb.append("  <input type=\"number\" class=\"form-control\" id=\"").
                        append(toVar(path)).append(f.getName()).append("\" v-model=\"").
                        append(path).append("__").append(f.getName()).append("\">\n");
                if (description.length() > 0) {
                    sb.append("  <small class=\"form-text text-muted\">").append(description).append("</small>\n");
                }
                sb.append("</div>\n");
            } else if (f.getType().equals(double.class)) {
                sb.append("<div class=\"form-group\">\n");
                sb.append("  <label for=\"").append(toVar(path)).append(f.getName()).append("\">").append(deCamelCase(f.getName())).append("</label>\n");
                sb.append("  <input type=\"text\" pattern=\"[0-9]*\\.[0-9]+\" class=\"form-control\" id=\"").
                        append(toVar(path)).append(f.getName()).append("\" v-model=\"").
                        append(path).append("__").append(f.getName()).append("\">\n");
                if (description.length() > 0) {
                    sb.append("  <small class=\"form-text text-muted\">").append(description).append("</small>\n");
                }
                sb.append("</div>\n");
            } else if (f.getType().equals(boolean.class)) {
                sb.append("<div class=\"form-group form-check\">\n");
                sb.append("  <input type=\"checkbox\" class=\"form-check-input\" id=\"").
                        append(toVar(path)).append(f.getName()).append("\" v-model=\"").
                        append(path).append("__").append(f.getName()).append("\">\n");
                sb.append("  <label class=\"form-check-label\" for=\"").append(toVar(path)).append(f.getName()).append("\">").append(deCamelCase(f.getName())).append("</label>\n");

                if (description.length() > 0) {
                    sb.append("  <small class=\"form-text text-muted\">").append(description).append("</small>\n");
                }
                sb.append("</div>\n");
            } else if (f.getType().equals(double[].class)) {
                sb.append("<div class=\"form-group\">\n");
                sb.append("  <label for=\"").append(toVar(path)).append(f.getName()).append("\">").append(deCamelCase(f.getName())).append("</label>\n");
                sb.append("  <input type=\"text\" pattern=\"([0-9]*\\.[0-9]+)(, ([0-9]*\\.[0-9]+))*\" class=\"form-control\" id=\"").
                        append(toVar(path)).append(f.getName()).append("\" v-model=\"").
                        append(path).append("__").append(f.getName()).append("\">\n");
                if (description.length() > 0) {
                    sb.append("  <small class=\"form-text text-muted\">").append(description).append(" (as comma separated values)</small>\n");
                }
                sb.append("</div>\n");

            } else if (f.getType().equals(List.class) || f.getType().equals(Set.class)) {
                Class listClass = (Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                if (knownConfigs.containsKey(listClass)) {
                    sb.append("<div class=\"card\">\n");
                    sb.append("  <h5 class=\"card-header\">").append(deCamelCase(f.getName()));
                    
                    sb.append("    <button class=\"btn btn-success\" v-on:click.prevent=\"add(")
                            .append(path).append(sep(path)).append(f.getName()).append(",'")
                            .append(f.getName()).append("')\"><i class=\"fas fa-plus-circle\"></i> Add</button>\n");
                    sb.append("</h5>\n");
                    sb.append("  <div class=\"card-body\">\n");
                    sb.append("    <ul class=\"list-group\">\n");
                    sb.append("      <li class=\"list-group-item\"  v-for=\"(").append(f.getName()).append("Item, ").append(f.getName()).append("Index) in ").append(path).append(sep(path)).append(f.getName()).append("\">\n");
                    sb.append("        <select class=\"form-control\" v-model=\"").
                            append(path).append(sep(path)).append(f.getName()).append("[").
                            append(f.getName()).append("Index].name\"")/* @change=\"changeComp(").
                            append(path).append(".").append(f.getName()).append("[").append(f.getName()).append("Index],").
                            append(defaultConfigObject(knownConfigs.get(listClass), mapper)).
                            append(",'").append(path.replace(".", "_")).append("_").
                            append(f.getName()).append("')\" id=\"").append(path.replace(".", "_")).append("_").
                            append(f.getName()).append("\">\n");*/.append(">\n");
                    for (Class c2 : knownConfigs.get(listClass)) {
                        sb.append("          <option value=\"").append(serviceName(c2)).append("\">").append(c2.getSimpleName()).append("</option>\n");
                    }
                    sb.append("        </select>\n");
                    for (Class c2 : knownConfigs.get(listClass)) {
                        sb.append("        <span v-if=\"").append(path).append(sep(path)).append(f.getName()).append("[").append(f.getName()).append("Index].name == \'").append(serviceName(c2)).append("\'\">");
                        try {
                            _java2vue(Class.forName(c2.getName() + "$Configuration"), sb, path + (sep(path)) + f.getName() + "[" + f.getName() + "Index].params");
                        } catch (ClassNotFoundException x) {
                            x.printStackTrace();
                        }
                        sb.append("        </span>");
                    }
                    sb.append("        <button class=\"btn btn-danger float-right\" v-on:click.prevent=\"remove(").append(path).append(sep(path)).append(f.getName()).append(",").append(f.getName()).append("Index)\"><i class=\"fas fa-minus-circle\"></i> Remove</button>\n");
                    sb.append("      </li>\n");
                    sb.append("    </ul>\n");
                    sb.append("  </div>\n");
                    sb.append("</div>\n");
                } else {
                    sb.append("<h5>").append(deCamelCase(f.getName()));
                    if(path.endsWith(".params")) {
                        String index = path.substring(path.lastIndexOf("[") + 1);
                        index = index.substring(0, index.length()-8);
                    sb.append("    <button class=\"btn btn-success\" v-on:click.prevent=\"addStr(").append(path.substring(0, path.lastIndexOf("[")))
                            .append(",").append(index)
                            .append(",'params__").append(f.getName()).append("','")
                            .append(path.substring(0, path.lastIndexOf("["))).append("')\"><i class=\"fas fa-plus-circle\"></i> Add</button>\n");
                    }
                    sb.append("</h5>\n");
                    sb.append("<ul class=\"list-group\">\n");
                    sb.append("<li class=\"list-group-item\"  v-for=\"(").append(f.getName()).append("Item, ").append(f.getName()).append("Index) in ").append(path).append("__").append(f.getName()).append("\">\n");
                    if(listClass.equals(String.class)) {
                        
                        sb.append("  <input type=\"text\" class=\"form-control\" id=\"").
                            append(toVar(path)).append(f.getName()).append("\" v-model=\"").
                            append(path).append("__").append(f.getName()).append("[").append(f.getName()).append("Index]").append("\">\n");
                        if (description.length() > 0) {
                            sb.append("  <small class=\"form-text text-muted\">").append(description).append("</small>\n");
                        }
                    } else if(listClass.isEnum()) {                        
                        sb.append("    <select class=\"form-control\"  v-model=\"").
                                append(path).append("__").append(f.getName()).append("[").append(f.getName()).append("Index]").append("\">\n");
                        for (Object o : listClass.getEnumConstants()) {
                            sb.append("      <option value=\"").append(o.toString()).append("\">").append(o.toString()).append("</option>\n");
                        }
                        sb.append("    </select>");
                        if (description.length() > 0) {
                            sb.append("  <small class=\"form-text text-muted\">").append(description).append("</small>\n");
                        }
                    } else {
                        sb.append("Unknown list class: " + listClass.getName());
                    }
                    sb.append("<button class=\"btn btn-danger float-right\" v-on:click.prevent=\"remove(").append(path).append("__").append(f.getName()).append(",").append(f.getName()).append("Index)\"><i class=\"fas fa-minus-circle\"></i> Remove</button>\n");
                    sb.append("</li>");
                    sb.append("</ul>");
                    
                }
            } else if (knownConfigs.containsKey(f.getType())) {
                sb.append("<div class=\"card\">\n");
                sb.append("  <h5 class=\"card-header\">").append(deCamelCase(f.getName())).append("</h5>\n");
                sb.append("  <div class=\"card-body\">\n");
                sb.append("  <select class=\"form-control\" v-model=\"").append(path).append(sep(path)).append(f.getName()).append("__name\">\n");
                for (Class c2 : knownConfigs.get(f.getType())) {
                    sb.append("    <option value=\"").append(serviceName(c2)).append("\"");
                    sb.append(">").append(c2.getSimpleName()).append("</option>\n");
                }
                sb.append("  </select>\n");
                for (Class c2 : knownConfigs.get(f.getType())) {
                    sb.append("<span v-if=\"").append(path).append(sep(path)).append(f.getName()).append("__name == \'").append(serviceName(c2)).append("\'\">");
                    try {
                        _java2vue(Class.forName(c2.getName() + "$Configuration"), sb, path + sep(path) + f.getName() + "__params");
                    } catch (ClassNotFoundException x) {
                        x.printStackTrace();
                    }
                    sb.append("</span>");
                }
                sb.append("  </div>\n");
                sb.append("</div>\n");
            } else if (f.getType().isEnum()) {
                sb.append("<div class=\"form-group\">\n");
                sb.append("  <label for=\"").append(toVar(path)).append("__").append(f.getName()).append("\">").append(deCamelCase(f.getName())).append("</label>\n");
                sb.append("    <select class=\"form-control\"  v-model=\"").
                        append(path).append("__").append(f.getName()).append("\">\n");
                for (Object o : f.getType().getEnumConstants()) {
                    sb.append("      <option value=\"").append(o.toString()).append("\">").append(o.toString()).append("</option>\n");
                }
                sb.append("    </select>");
                if (description.length() > 0) {
                    sb.append("  <small class=\"form-text text-muted\">").append(description).append("</small>\n");
                }
                sb.append("</div>\n");
            } else {
                sb.append("<div>Unrecognized: ").append(f.getName()).append(" of type ").append(f.getType().getName()).append("</div>");
            }
        }
    }

    private static String sep(String path) {
        if(path.equals("")) {
            return "";
        } else if(path.equals("config")) {
            return ".";
        } else {
            return "__";
        }
    }
    
    private static Object annoDefaultValue(ConfigurationParameter param, ObjectMapper mapper) {
        Object defaultValue;
        try {
            if (param != null) {
                String s = param.defaultValue();
                if (!s.startsWith("[") && !s.startsWith("\"") && !s.startsWith("{") && !s.equals("null") && !s.equals("true") && !s.equals("false")) {
                    defaultValue = mapper.readValue("\"" + s + "\"", Object.class);
                } else {
                    defaultValue = mapper.readValue(s, Object.class);
                }
            } else {
                defaultValue = "";
            }
        } catch (IOException x) {
            x.printStackTrace();
            defaultValue = "";
        }
        return defaultValue;
    }

    private static String deCamelCase(String raw) {
        if (raw.length() > 0) {
            String s = raw.replaceAll("(?<=[^\\p{IsUpper}])(\\p{IsUpper})", " $1").replaceAll("(\\p{IsUpper})(?=[^\\p{IsUpper}])", " $1");
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        } else {
            return "";
        }
    }

    private static String toVar(String v) {
        return v.replace('.', '_').replaceAll("\\[[^\\]]\\]", "");
    }

    private static HashMap<Class, Class[]> knownConfigs = new HashMap<Class, Class[]>() {
        {
            put(Configuration.BlockingStrategyConfiguration.class, Configuration.knownBlockingStrategies);
            put(Configuration.LensConfiguration.class, Configuration.knownLenses);
            put(Configuration.TextFeatureConfiguration.class, Configuration.knownTextFeatures);
            put(Configuration.GraphFeatureConfiguration.class, Configuration.knownGraphFeatures);
            put(Configuration.ScorerConfiguration.class, Configuration.knownScorers);
            put(Configuration.MatcherConfiguration.class, Configuration.knownMatchers);
            put(Configuration.ConstraintConfiguration.class, Configuration.knownConstraints);
        }
    };

    private static String serviceName(Class c) {
        return c.getName().replaceAll("^org\\.insightcentre\\.uld\\.naisc\\.", "");
    }

    private static String defaultConfigObject(Class[] configs, ObjectMapper mapper) {
        HashMap<String, HashMap<String, Object>> data = new HashMap<>();
        for (Class c : configs) {
            try {
                HashMap<String, Object> data2 = new HashMap<>();
                data.put(serviceName(c), data2);
                for (Field f : Class.forName(c.getName() + "$Configuration").getFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        ConfigurationParameter param = f.getAnnotation(ConfigurationParameter.class);
                        Object defaultValue;
                        try {
                            if (param != null) {
                                String s = param.defaultValue();
                                if (!s.startsWith("[") && !s.startsWith("\"") && !s.startsWith("{") && !s.equals("null") && !s.equals("true") && !s.equals("false")) {
                                    defaultValue = mapper.readValue("\"" + s + "\"", Object.class);
                                } else {
                                    defaultValue = mapper.readValue(s, Object.class);
                                }
                            } else {
                                defaultValue = "";
                            }
                        } catch (IOException x) {
                            throw new RuntimeException(x);
                        }
                        data2.put(f.getName(), defaultValue);
                    }
                }
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            return mapper.writeValueAsString(data).replaceAll("\"", "'");
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String defaultObject(Class c, ObjectMapper mapper) throws ClassNotFoundException, JsonProcessingException {
        HashMap<String, Object> data2 = new HashMap<>();
        for (Field f : Class.forName(c.getName() + "$Configuration").getFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                ConfigurationParameter param = f.getAnnotation(ConfigurationParameter.class);
                Object defaultValue;
                try {
                    if (param != null) {
                        String s = param.defaultValue();
                        if (!s.startsWith("[") && !s.startsWith("\"") && !s.startsWith("{") && !s.equals("null") && !s.equals("true") && !s.equals("false")) {
                            defaultValue = mapper.readValue("\"" + s + "\"", Object.class);
                        } else {
                            defaultValue = mapper.readValue(s, Object.class);
                        }
                    } else {
                        defaultValue = "";
                    }
                } catch (IOException x) {
                    throw new RuntimeException(x);
                }
                data2.put(f.getName(), defaultValue);
            }
        }
        return mapper.writeValueAsString(data2);
    }

    public static String watchCode(Class configClass) {

        StringBuilder sb = new StringBuilder();
        _watchCode(configClass, sb, "config");
        return sb.toString();
    }

    private static void _watchCode(Class configClass, StringBuilder sb, String path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (Field f : configClass.getFields()) {
                ConfigurationParameter param = f.getAnnotation(ConfigurationParameter.class);
                Object defaultValue = annoDefaultValue(param, mapper);
                if (Modifier.isStatic(f.getModifiers())) {

                } else if (f.getType().equals(List.class) || f.getType().equals(Set.class)) {
                    Class listClass = (Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                    if (knownConfigs.containsKey(listClass)) {
                        sb.append("if(!this.").append(path).append(".").append(f.getName()).append(") {\n");
                        sb.append("this.").append(path).append(".").append(f.getName()).
                                append(" = ").append("[];\n");
                        sb.append("} else {\n");
                        sb.append("for(").append(f.getName()).append("Idx = 0; ").
                                append(f.getName()).append("Idx < this.").append(path)
                                .append(".").append(f.getName()).append(".length; ").append(f.getName())
                                .append("Idx++) {\n");
                        for(Class c2 : knownConfigs.get(listClass)) {
                            _watchCode(Class.forName(c2.getName() + "$Configuration"), sb, path + "." + f.getName() + "[" + f.getName() + "Idx].params");
                        }
                        sb.append("}\n");
                        sb.append("}\n");
                    } else {
                        sb.append("if(!this.").append(path).append(".").append(f.getName()).append(") {\n");
                        sb.append("this.").append(path).append(".").append(f.getName()).append(" = [];\n");
                        sb.append("}\n");
                    }
                } else if (knownConfigs.containsKey(f.getType())) {
                    sb.append("if(!this.").append(path).append(".").append(f.getName()).append(") {\n");
                    Class c0 = knownConfigs.get(f.getType())[0];
                    sb.append("this.").append(path).append(".").append(f.getName()).append(" = {\"name\":\"")
                            .append(serviceName(c0)).append("\",\"params\":")
                            .append(defaultObject(c0, mapper)).append("};\n");
                    sb.append("} else {\n");
                     for(Class c2 : knownConfigs.get(f.getType())) {
                         sb.append("if(this.").append(path).append(".").append(f.getName()).append(".name === \"").append(serviceName(c2)).append("\") {\n");
                            _watchCode(Class.forName(c2.getName() + "$Configuration"), sb, path + "." + f.getName() + ".params");
                         sb.append("}\n");
                     }
                    sb.append("}\n");
                }
            }
        } catch (ClassNotFoundException | JsonProcessingException | SecurityException x) {
            throw new RuntimeException(x);
        }
    }
}
