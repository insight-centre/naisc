package org.insightcentre.uld.naisc.matcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.main.ExecuteListener;

import java.io.IOException;
import java.util.Map;

/**
 * Implements matching through an external REST service
 */
public class ExternalMatcher implements MatcherFactory {
    @Override
    public String id() {
        return "external";
    }

    @Override
    public Matcher makeMatcher(Map<String, Object> params) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if(config.path == null) {
            return new ExternalMatcherImpl(String.format("%s/naisc/%s/match"));
        } else {
            return new ExternalMatcherImpl(config.path);
        }
    }

    @ConfigurationClass("Use an external matcher by calling a REST service")
    /**
     * Configuration for the external matcher
     */
    public static class Configuration {
        @ConfigurationParameter(description = "Endpoint for scorer service, for example http://localhost:8080")
        /**
         * The endpoint
         */
        public String endpoint = "http://localhost:8080";

        @ConfigurationParameter(description = "The configuration parameter for the external parameter")
        /**
         * The configuration name (for the external service)
         */
        public String configName = "default";

        @ConfigurationParameter(description = "The path or null if the path is $endpoint/naisc/$configName/match")
        /**
         * The path or null if the path is $endpoint/naisc/$configName/match
         */
        public String path;

    }

    private static class ExternalMatcherImpl implements Matcher {
        private final String endpoint;

        public ExternalMatcherImpl(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public AlignmentSet alignWith(AlignmentSet matches, AlignmentSet partialAlign, ExecuteListener listener) {
            if(partialAlign.size() > 0) {
                throw new RuntimeException("External matcher does not support the use of a partial alignment (e.g., in semi-supervised mode)");
            }

            try(CloseableHttpClient client = HttpClients.createDefault()) {
                ObjectMapper mapper = new ObjectMapper();
                HttpPost post = new HttpPost(endpoint);
                StringEntity entity = new StringEntity(mapper.writeValueAsString(matches));
                entity.setContentType("application/json");
                post.setEntity(entity);
                ResponseHandler<AlignmentSet> handler = new ResponseHandler<AlignmentSet>() {
                    @Override
                    public AlignmentSet handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                        int status = httpResponse.getStatusLine().getStatusCode();
                        if(status == 200) {
                            return mapper.readValue(httpResponse.getEntity().getContent(), AlignmentSet.class);
                        } else {
                            throw new RuntimeException(String.format("%s returned status code %d", endpoint, status));
                        }
                    }
                };
                return client.execute(post, handler);
            } catch(IOException x) {
                throw new RuntimeException(String.format("Could not access external service %s", endpoint), x);
            }
        }
    }
}
