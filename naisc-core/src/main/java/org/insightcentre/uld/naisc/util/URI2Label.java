package org.insightcentre.uld.naisc.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

/**
 * Covert a URI to a human-readable label (if possible)
 * @author John McCrae
 */
public class URI2Label {

    public static String fromURI(String s) {
        try {
            URI uri = URI.create(s);
            final String raw;
            if (uri.getFragment() != null) {
                raw = uri.getFragment();
            } else {

                String path = uri.getPath();
                if (path == null) {
                    raw = uri.getSchemeSpecificPart();
                } else if (path.lastIndexOf("/") >= 0) {
                    raw = path.substring(path.lastIndexOf("/") + 1);
                } else if (path.lastIndexOf("\\") >= 0) {
                    raw = path.substring(path.lastIndexOf("\\") + 1);
                } else {
                    raw = path;
                }
            }
            final String clean;
            try {
                clean = URLDecoder.decode(deCamelCase(raw).replace("_", " "), "UTF-8");
            } catch (UnsupportedEncodingException x) {
                return "";
            }
            return clean;
        } catch (Exception x) {
            return "";
        }
    }
    
        public static String deCamelCase(String raw) {
        return raw.replaceAll("(?<=[^\\p{IsUpper}])(\\p{IsUpper})", " $1").replaceAll("(\\p{IsUpper})(?=[^\\p{IsUpper}])", " $1");
    }
}
