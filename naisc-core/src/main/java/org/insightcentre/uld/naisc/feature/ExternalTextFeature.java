package org.insightcentre.uld.naisc.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.insightcentre.uld.naisc.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class ExternalTextFeature implements TextFeatureFactory {
    @Override
    public TextFeature makeFeatureExtractor(Set<String> tags, Map<String, Object> params) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if(config.path == null) {
            return new ExternalTextFeatureImpl(String.format("%s/naisc/%s/text_features", config.endpoint, config.configName), config.id, tags);
        } else {
            return new ExternalTextFeatureImpl(config.path, config.id, tags);
        }
    }

    @ConfigurationClass("An external text feature called through a REST endpoint")
    public static class Configuration {
        @ConfigurationParameter(description = "Endpoint for text feature service, for example http://localhost:8080")
        public String endpoint = "http://localhost:8080";

        @ConfigurationParameter(description = "The configuration parameter for the external parameter")
        public String configName = "default";

        @ConfigurationParameter(description = "The path or null if the path is $endpoint/naisc/$configName/text_features")
        public String path;

        @ConfigurationParameter(description = "The name of this feature")
        public String id = "external";
    }

    private static class ExternalTextFeatureImpl implements TextFeature {
        private final String path;
        private final String id;
        private final Set<String> tags;

        public ExternalTextFeatureImpl(String path, String id, Set<String> tags) {
            this.path = path;
            this.id = id;
            this.tags = tags;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Feature[] extractFeatures(LensResult facet, NaiscListener log) {
            try(CloseableHttpClient client = HttpClients.createDefault()) {
                ObjectMapper mapper = new ObjectMapper();
                HttpPost post = new HttpPost(path);
                StringEntity entity = new StringEntity(mapper.writeValueAsString(facet), "UTF-8");
                entity.setContentType("application/json");
                entity.setContentEncoding("UTF-8");
                post.setEntity(entity);
                ResponseHandler<Feature[]> handler = new ResponseHandler<Feature[]>() {
                    @Override
                    public Feature[] handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                        int status = httpResponse.getStatusLine().getStatusCode();
                        if(status == 200) {
                            return mapper.readValue(httpResponse.getEntity().getContent(), mapper.getTypeFactory().constructArrayType(Feature.class));
                        } else {
                            System.err.println(mapper.writeValueAsString(facet));
                            throw new RuntimeException(String.format("%s returned status code %d", path, status));
                        }
                    }
                };
                return client.execute(post, handler);
            } catch(IOException x) {
                throw new RuntimeException(String.format("Could not access external service %s", path), x);
            }
        }

        @Override
        public Set<String> tags() {
            return tags;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
