package org.insightcentre.uld.naisc.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.util.Lazy;
import org.insightcentre.uld.naisc.util.StringPair;

import java.io.IOException;
import java.util.Map;

public class ACDHFeature implements GraphFeatureFactory {
    @ConfigurationClass("This class implements ACDH's RoBERTA based classifier developed for the ELEXIS project. The actual service must be running as an independent service")
    public static class Configuration {
        @ConfigurationParameter(description = "The URL for the endpoint of the ACDH classifier")
        public String endpoint;

        @ConfigurationParameter(description = "The property used to indicate the lemma")
        public String lemmaURL = "http://www.w3.org/2000/01/rdf-schema#label";

        @ConfigurationParameter(description = "The property used for part-of-speech")
        public String posURL = "http://www.lexinfo.net/ontology/3.0/lexinfo#partOfSpeech";
    }

    @Override
    public GraphFeature makeFeature(Dataset sparqlData, Map<String, Object> params, Lazy<Analysis> analysis, AlignmentSet prelinking, NaiscListener listener) {
        Configuration config = new ObjectMapper().convertValue(params, Configuration.class);
        if(config.endpoint == null) {
            throw new RuntimeException("Endpoint for ACDH service must be specified");
        } //else if(config.)
        throw new RuntimeException("TODO");
    }

    private static class ACDHFeatureImpl implements GraphFeature {
        private final Dataset dataset;
        private final String lemmaURL, defURL, posURL;
        private final String path;
        private final String classifier;

        public ACDHFeatureImpl(Dataset dataset, String lemmaURL, String defURL, String posURL, String path, String classifier) {
            this.dataset = dataset;
            this.lemmaURL = lemmaURL;
            this.defURL = defURL;
            this.posURL = posURL;
            this.path = path;
            this.classifier = classifier;
        }

        @Override
        public String id() {
            return "acdh";
        }

        @Override
        public Feature[] extractFeatures(URIRes entity1, URIRes entity2, NaiscListener log) {
            StringPair lemma1 = extractLemma(entity1, log), lemma2 = extractLemma(entity2, log);
            String def1 = extractDefinition(entity1, log), def2 = extractDefinition(entity2, log);
            String pos = extractPos(entity1, log);

            if(def1 == null) {
                log.message(NaiscListener.Stage.SCORING, NaiscListener.Level.WARNING,
                        String.format("Could not infer a definition for %s", entity1.getURI()));
                def1 = "";
            }

            if(def2 == null) {
                log.message(NaiscListener.Stage.SCORING, NaiscListener.Level.WARNING,
                        String.format("Could not infer a definition for %s", entity2.getURI()));
                def2 = "";
            }

            String lemma = lemma1 != null ? lemma1._1 :
                    lemma2 != null ? lemma2._1 : "foobar";
            String lang = lemma1 != null ? lemma1._2 :
                    lemma2 != null ? lemma2._2 : "en";

            try(CloseableHttpClient client = HttpClients.createDefault()) {
                ObjectMapper mapper = new ObjectMapper();
                HttpPost post = new HttpPost(path);
                StringEntity entity = new StringEntity(mapper.writeValueAsString(
                        new ACDHMessageBody(classifier, new ACDHMessagePair(
                                lemma, lang, pos == null ? "noun" : pos,
                        def1, def2))));
                entity.setContentType("application/json");
                post.setEntity(entity);
                ResponseHandler<Feature[]> handler = new ResponseHandler<Feature[]>() {
                    @Override
                    public Feature[] handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                        int status = httpResponse.getStatusLine().getStatusCode();
                        if(status == 200) {
                            ACDHResponse[] responses =  mapper.readValue(httpResponse.getEntity().getContent(),
                                    mapper.getTypeFactory().constructArrayType(ACDHResponse.class));
                            Feature[] features = new Feature[responses.length];
                            for(int i = 0; i < features.length; i++) {
                                features[i] = new Feature("acdh_" + responses[i].alignment,
                                        Double.parseDouble(responses[i].probability));
                            }
                            return features;
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

        private String extractPos(URIRes entity1, NaiscListener listener) {
            if(posURL != null) {
                NodeIterator iter1 = dataset.listObjectsOfProperty(entity1.toJena(dataset), dataset.createProperty(posURL));
                if (iter1.hasNext()) {
                    RDFNode n = iter1.next();
                    if (n.isLiteral()) {
                        return n.asLiteral().getString();
                    } else if(n.isURIResource()) {
                        String uri = n.asResource().getURI();
                        if(uri.indexOf("#") > 0) {
                            return uri.substring(uri.lastIndexOf('#')+1);
                        } else if (uri.indexOf("/") > 0) {
                            return uri.substring(uri.lastIndexOf('/')+1);
                        } else {
                            listener.message(NaiscListener.Stage.SCORING, NaiscListener.Level.WARNING,
                                    String.format("The value of property %s for node %s was not interpretable as a POS",
                                            lemmaURL, entity1.getURI()));
                            return n.toString();
                        }
                    }
                }
            }
            return null;
        }

        private String extractDefinition(URIRes entity1, NaiscListener listener) {
                NodeIterator iter1 = dataset.listObjectsOfProperty(entity1.toJena(dataset), dataset.createProperty(defURL));
                if (iter1.hasNext()) {
                    RDFNode n = iter1.next();
                    if (n.isLiteral()) {
                        return n.asLiteral().getString();
                    } else {
                        listener.message(NaiscListener.Stage.SCORING, NaiscListener.Level.WARNING, String.format("The value of property %s for node %s was not a literal!", lemmaURL, entity1.getURI()));
                        return n.toString();
                    }
                }
            return null;
        }

        private StringPair extractLemma(URIRes entity1, NaiscListener listener) {
            if(lemmaURL != null) {
                NodeIterator iter1 = dataset.listObjectsOfProperty(entity1.toJena(dataset), dataset.createProperty(lemmaURL));
                if (iter1.hasNext()) {
                    RDFNode n = iter1.next();
                    if (n.isLiteral()) {
                        return new StringPair(n.asLiteral().getString(),n.asLiteral().getLanguage());
                    } else {
                        listener.message(NaiscListener.Stage.SCORING, NaiscListener.Level.WARNING, String.format("The value of property %s for node %s was not a literal!", lemmaURL, entity1.getURI()));
                        return new StringPair(n.toString(), null);
                    }
                }
            }
            return null;
        }
    }

    private static class ACDHMessageBody {
        public String classifier;
        public ACDHMessagePair pair;

        public ACDHMessageBody(String classifier, ACDHMessagePair pair) {
            this.classifier = classifier;
            this.pair = pair;
        }

        public ACDHMessageBody() {
        }
    }

    private static class ACDHMessagePair {
        public String headword, pos, lang, def1, def2;

        public ACDHMessagePair() {

        }

        public ACDHMessagePair(String headword, String pos, String lang, String def1, String def2) {
            this.headword = headword;
            this.pos = pos;
            this.lang = lang;
            this.def1 = def1;
            this.def2 = def2;
        }
    }

    private static class ACDHResponse {
        public String alignment;
        public String probability; // This could be a bug on ACDH's part
    }
}
