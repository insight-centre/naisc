package org.insightcentre.uld.naisc.lens;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.util.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Extract text from the data using an external service
 *
 * @author John McCrae
 */
public class ExternalLens implements LensFactory {
    @Override
    public Lens makeLens(Dataset dataset, Map<String, Object> params) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if(config.path == null) {
            return new ExternalLensImpl(String.format("%s/naisc/%s/extract_text"));
        } else {
            return new ExternalLensImpl(config.path);
        }
    }

    @ConfigurationClass("An external lens called through a REST endpoint")
    public static class Configuration {
        @ConfigurationParameter(description = "Endpoint for lens service, for example http://localhost:8080")
        public String endpoint = "http://localhost:8080";

        @ConfigurationParameter(description = "The configuration parameter for the external parameter")
        public String configName = "default";

        @ConfigurationParameter(description = "The path or null if the path is $endpoint/naisc/$configName/extract_text")
        public String path;
    }

    private static class ExternalLensImpl implements Lens {
        private final String endpoint;

        public ExternalLensImpl(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public Collection<LensResult> extract(URIRes entity1, URIRes entity2, NaiscListener log) {
            try(CloseableHttpClient httpclient = HttpClients.createDefault()) {
                final ObjectMapper mapper = new ObjectMapper();
                HttpPost post = new HttpPost(endpoint);
                post.setEntity(new StringEntity(mapper.writeValueAsString(new ResourcePair(entity1, entity2))));
                ResponseHandler<Collection<LensResult>> handler = new ResponseHandler<Collection<LensResult>>() {
                    @Override
                    public Collection<LensResult> handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                        int status = httpResponse.getStatusLine().getStatusCode();
                        if(status == 200) {
                            return mapper.readValue(httpResponse.getEntity().getContent(), mapper.getTypeFactory().constructCollectionType(ArrayList.class, LensResult.class));
                        } else {
                            throw new RuntimeException("Could not get response from " + endpoint + " response code=" + status);
                        }
                    }
                };
                return httpclient.execute(post, handler);
            } catch (IOException x) {
                throw new RuntimeException(String.format("Could not access external service %s", endpoint), x);
            }
        }
    }
}
