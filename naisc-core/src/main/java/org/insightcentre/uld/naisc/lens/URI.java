package org.insightcentre.uld.naisc.lens;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.monnetproject.lang.Language;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.ConfigurationParameter;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.LensFactory;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * This lens assumes that the label is encoded within the URI and extracts
 * it appropriately.
 * 
 * @author John McCrae
 */
public class URI implements LensFactory {

    public static String deCamelCase(String raw) {
        return raw.replaceAll("(?<=[^\\p{IsUpper}])(\\p{IsUpper})", " $1").replaceAll("(\\p{IsUpper})(?=[^\\p{IsUpper}])", " $1");
    }

    @Override
    public Lens makeLens(String tag, Dataset dataset, Map<String, Object> params) {
        final Model sparqlData = dataset.asModel().getOrExcept(new RuntimeException("Cannot apply method to SPARQL endpoint"));
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        return new URIImpl(tag, config.location, config.form, config.separator);
    }

    /**
     * Configuration of the URI label extractor.
     */
    public static class Configuration {
        /**
         * The location of the label in the URL.
         */
        @ConfigurationParameter(description = "The location of the label in the URL")
        public LabelLocation location = LabelLocation.infer;
        /**
         * The form (camelCased, under_scored) of the label.
         */
        @ConfigurationParameter(description = "The form (camelCased, under_scored) of the label")
        public LabelForm form = LabelForm.smart;
        /**
         * The character that separates words in the label
         */
        @ConfigurationParameter(description = "The character that separates words in the label")
        public String separator = "_";
    }
    
    /**
     * Locations where a label may be in a URL
     */
    public enum LabelLocation {
        /**
         * The label is in the fragment. e.g., http://www.example.com/page#Label
         */
        fragment,
        /**
         * The label is the end of the path. e.g., http://www.example.com/page/Label
         */
        endOfPath,
        /**
         * Infer the label location. e.g., assume it is the fragment if there is a fragment, otherwise use end of path
         */
        infer
    }
    
    /**
     * How a label may be encoded in a URL
     */
    public enum LabelForm {
        /**
         * Camel cased. Assumes that a space should be inserted before every upper
         * case letter preceded or followed by an non-upper-case, e.g.,
         * <br>
         * customXMLFile ⇒ custom XML file
         */
        camelCased,
        /**
         * Underscore separated. Simply replace all '_' with spaces; the character 
         * used can be overridden in the configuration.
         * <br>
         * custom_xml_file ⇒ custom xml file
         */
        underscored,
        /**
         * URL Encoded. Uses URL encoding scheme, e.g.,
         * 
         * custom+%5Bxml%5D+file ⇒ custom [xml] file
         * 
         */
        urlEncoded,
        /**
         * Guess the encoding. This will apply all heuristics to decode the label
         */
        smart
    }
    
    private static class URIImpl implements Lens {
        private final String tag;
        private final LabelLocation location;
        private final LabelForm form;
        private final String separator;

        public URIImpl(String tag, LabelLocation location, LabelForm form, String separator) {
            this.tag = tag;
            this.location = location;
            this.form = form;
            this.separator = separator;
        }

        @Override
        public String id() {
            return "lens";
        }

        @Override
        public Option<LangStringPair> extract(Resource entity1, Resource entity2, NaiscListener log) {
            if(entity1.isURIResource() && entity2.isURIResource()) {
                try {
                    java.net.URI uri1 = new java.net.URI(entity1.getURI());
                    java.net.URI uri2 = new java.net.URI(entity2.getURI());
                    String raw1 = getRaw(uri1, location);
                    String raw2 = getRaw(uri2, location);
                    if(raw1 != null && raw2 != null) {
                        return new Some<>(new LangStringPair(Language.UNDEFINED, Language.UNDEFINED, getLabel(raw1,form, separator), getLabel(raw2, form, separator)));
                    } else {
                        return new None<>();
                    }
                } catch(URISyntaxException x) {
                    throw new RuntimeException("Bad URI in dataset", x);
                }
            }
            return new None<>();
        }

        @Override
        public String tag() {
            return tag;
        }
        
    }
    
    private static String getRaw(java.net.URI uri, LabelLocation location) {
        switch(location) {
            case fragment:
                return uri.getFragment();
            case infer:
                if(uri.getFragment() != null) {
                    return uri.getFragment();
                }
            case endOfPath:
                String path = uri.getPath();
                if(path.lastIndexOf("/") >= 0) {
                    return path.substring(path.lastIndexOf("/") + 1);
                } else if (path.lastIndexOf("\\") >= 0) {
                    return path.substring(path.lastIndexOf("\\") + 1);
                } else {
                    return path;
                }
            default:
                throw new RuntimeException("Location null or unknown");
        }
    }
    
    private static String getLabel(String raw, LabelForm method, String separator) {
        switch(method) {
            case camelCased:
                return deCamelCase(raw);
            case underscored:
                return raw.replace(separator, " ");
            case urlEncoded:
                try {
                    return URLDecoder.decode(raw, "UTF-8");
                } catch(UnsupportedEncodingException x) {
                    throw new RuntimeException();
                }
            case smart:
                try {
                    return URLDecoder.decode(deCamelCase(raw).replace(separator, " "), "UTF-8");
                } catch(UnsupportedEncodingException x) {
                    throw new RuntimeException();
                }
        }
        throw new RuntimeException("method null or unknown");
    }
}
