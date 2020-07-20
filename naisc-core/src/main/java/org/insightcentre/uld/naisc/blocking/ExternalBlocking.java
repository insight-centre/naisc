package org.insightcentre.uld.naisc.blocking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.util.Lazy;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Blocking from an external service called by REST
 *
 */
public class ExternalBlocking implements BlockingStrategyFactory {
    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params, Lazy<Analysis> analysis, NaiscListener listener) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if(config.path == null) {
            return new ExternalBlockingImpl(String.format("%s/naisc/%s/block", config.endpoint, config.configName));
        } else {
            return new ExternalBlockingImpl(config.path);
        }
    }

    @ConfigurationClass("An external blocking service called through a REST endpoint")
    public static class Configuration {
        @ConfigurationParameter(description = "Endpoint for blocking service, for example http://localhost:8080")
        public String endpoint = "http://localhost:8080";

        @ConfigurationParameter(description = "The configuration parameter for the external parameter")
        public String configName = "default";

        @ConfigurationParameter(description = "The path or null if the path is $endpoint/naisc/$configName/block")
        public String path;
    }

    private static class ExternalBlockingImpl implements BlockingStrategy {
        private final String endpoint;

        public ExternalBlockingImpl(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public Collection<Blocking> block(Dataset left, Dataset right, NaiscListener log) {

            try(CloseableHttpClient httpclient = HttpClients.createDefault()) {
                URIBuilder builder = new URIBuilder(endpoint);
                builder.setParameter("left", left.id());
                builder.setParameter("right", right.id());
                HttpGet get = new HttpGet(builder.build());
                ResponseHandler<List<Blocking>> responseHandler = new ResponseHandler<List<Blocking>>() {
                    @Override
                    public List<Blocking> handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                        int status = response.getStatusLine().getStatusCode();
                        if(status == 200) {
                            ObjectMapper mapper = new ObjectMapper();
                            return mapper.readValue(response.getEntity().getContent(), mapper.getTypeFactory().constructCollectionType(ArrayList.class, Blocking.class));
                        } else {
                            throw new RuntimeException(String.format("Failed to read from %s. Status=%d", endpoint, status));
                        }
                    }
                };
                return httpclient.execute(get, responseHandler);
            } catch(IOException| URISyntaxException x) {
                throw new RuntimeException(String.format("Failed to read from %s", endpoint), x);
            }
        }
    }
}
