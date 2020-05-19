package org.insightcentre.uld.naisc.lens;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.Option;

import java.io.IOException;
import java.util.Map;

/**
 * Extract text from the data using an external service
 *
 * @author John McCrae
 */
public class ExternalLens implements LensFactory {
    @Override
    public Lens makeLens(String tag, Dataset dataset, Map<String, Object> params) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if(config.path == null) {
            return new ExternalLensImpl(String.format("%s/naisc/%s/extract_text"), tag, config.id);
        } else {
            return new ExternalLensImpl(config.path, tag, config.id);
        }
    }

    public static class Configuration {
        @ConfigurationParameter(description = "Endpoint for blocking service, for example http://localhost:8080")
        public String endpoint = "http://localhost:8080";

        @ConfigurationParameter(description = "The configuration parameter for the external parameter")
        public String configName = "default";

        @ConfigurationParameter(description = "The path or null if the path is $endpoint/naisc/$configName/extract_text")
        public String path;

        @ConfigurationParameter(description = "The identifier for this lens (used to name features)")
        public String id = "external";
    }

    private static class ExternalLensImpl implements Lens {
        private final String endpoint;
        private final String tag;
        private final String id;

        public ExternalLensImpl(String endpoint, String tag, String id) {
            this.endpoint = endpoint;
            this.tag = tag;
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Option<LensResult> extract(Resource entity1, Resource entity2, NaiscListener log) {
            try(CloseableHttpClient httpclient = HttpClients.createDefault()) {
                ObjectMapper mapper = new ObjectMapper();
                HttpPost post = new HttpPost(endpoint);
                post.setEntity(new StringEntity("foo"));
                throw new UnsupportedOperationException("TODO");

            } catch (IOException x) {
                throw new RuntimeException(String.format("Could not access external service %s", endpoint), x);
            }
        }

        @Override
        public String tag() {
            return tag;
        }
    }
}
