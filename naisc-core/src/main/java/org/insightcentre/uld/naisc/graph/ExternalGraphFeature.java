package org.insightcentre.uld.naisc.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.lens.ExternalLens;
import org.insightcentre.uld.naisc.util.Lazy;

import java.io.IOException;
import java.util.Map;

public class ExternalGraphFeature implements GraphFeatureFactory {
    @Override
    public GraphFeature makeFeature(Dataset sparqlData, Map<String, Object> params, Lazy<Analysis> analysis, AlignmentSet prelinking, NaiscListener listener) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if(config.path == null) {
            return new ExternalGraphFeatureImpl(String.format("%s/naisc/%s/graph_features", config.endpoint, config.configName));
        } else {
            return new ExternalGraphFeatureImpl(config.path);
        }

    }


    @ConfigurationClass("An external graph feature called through a REST endpoint")
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

    private static class ExternalGraphFeatureImpl implements GraphFeature {
        private final String path;

        public ExternalGraphFeatureImpl(String path) {
            this.path = path;
        }

        @Override
        public String id() {
            return "external";
        }

        @Override
        public Feature[] extractFeatures(URIRes entity1, URIRes entity2, NaiscListener log) {
            try(CloseableHttpClient client = HttpClients.createDefault()) {
                ObjectMapper mapper = new ObjectMapper();
                HttpPost post = new HttpPost(path);
                StringEntity entity = new StringEntity(mapper.writeValueAsString(new ResourcePair(entity1, entity2)));
                entity.setContentType("application/json");
                post.setEntity(entity);
                ResponseHandler<Feature[]> handler = new ResponseHandler<Feature[]>() {
                    @Override
                    public Feature[] handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                        int status = httpResponse.getStatusLine().getStatusCode();
                        if(status == 200) {
                            return mapper.readValue(httpResponse.getEntity().getContent(), mapper.getTypeFactory().constructArrayType(Feature.class));
                        } else {
                            throw new RuntimeException(String.format("%s returned status code %d", path, status));
                        }
                    }
                };
                return client.execute(post, handler);
            } catch(IOException x) {
                throw new RuntimeException(String.format("Could not access external service %s", path), x);
            }
        }
    }
}
