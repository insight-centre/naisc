package org.insightcentre.uld.naisc.scorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.graph.ExternalGraphFeature;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExternalScorer implements ScorerFactory {
    public String id() {
        return "external";
    }

    @Override
    public Scorer makeScorer(Map<String, Object> params, File modelPath) throws IOException {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if(config.path == null) {
            return new ExternalScorerImpl(String.format("%s/naisc/%s/score", config.endpoint, config.configName));
        } else {
            return new ExternalScorerImpl(config.path);
        }
    }

    @Override
    public Option<ScorerTrainer> makeTrainer(Map<String, Object> params, String property, File modelPath) {
        return new None<>();
    }

    @ConfigurationClass("An external scorer called through a REST endpoint")
    public static class Configuration {
        @ConfigurationParameter(description = "Endpoint for scorer service, for example http://localhost:8080")
        public String endpoint = "http://localhost:8080";

        @ConfigurationParameter(description = "The configuration parameter for the external parameter")
        public String configName = "default";

        @ConfigurationParameter(description = "The path or null if the path is $endpoint/naisc/$configName/score")
        public String path;
    }

    private static class ExternalScorerImpl implements Scorer {
        private final String endpoint;

        public ExternalScorerImpl(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public List<ScoreResult> similarity(FeatureSet features, NaiscListener log) {
            try(CloseableHttpClient client = HttpClients.createDefault()) {
                ObjectMapper mapper = new ObjectMapper();
                HttpPost post = new HttpPost(endpoint);
                StringEntity entity = new StringEntity(mapper.writeValueAsString(features));
                entity.setContentType("application/json");
                post.setEntity(entity);
                ResponseHandler<List<ScoreResult>> handler = new ResponseHandler<List<ScoreResult>>() {
                    @Override
                    public List<ScoreResult> handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                        int status = httpResponse.getStatusLine().getStatusCode();
                        if(status == 200) {
                            return mapper.readValue(httpResponse.getEntity().getContent(), mapper.getTypeFactory().constructCollectionType(List.class, ScoreResult.class));
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

        @Override
        public void close() throws IOException {

        }
    }
}
