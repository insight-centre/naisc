package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.insightcentre.uld.naisc.AlignmentSet;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.BlockingStrategyFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.LensFactory;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.MatcherFactory;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.ScorerFactory;
import org.insightcentre.uld.naisc.feature.*;
import org.insightcentre.uld.naisc.rescaling.MinMax;
import org.insightcentre.uld.naisc.rescaling.NoRescaling;
import org.insightcentre.uld.naisc.scorer.MergedScorer;
import org.insightcentre.uld.naisc.util.Services;
import org.insightcentre.uld.naisc.ScorerTrainer;
import org.insightcentre.uld.naisc.TextFeature;
import org.insightcentre.uld.naisc.TextFeatureFactory;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.GraphFeatureFactory;
import org.insightcentre.uld.naisc.NaiscListener;
import org.insightcentre.uld.naisc.Rescaler;
import org.insightcentre.uld.naisc.analysis.Analysis;
import org.insightcentre.uld.naisc.blocking.All;
import org.insightcentre.uld.naisc.blocking.ApproximateStringMatching;
import org.insightcentre.uld.naisc.blocking.Automatic;
import org.insightcentre.uld.naisc.blocking.IDMatch;
import org.insightcentre.uld.naisc.blocking.LabelMatch;
import org.insightcentre.uld.naisc.blocking.Path;
import org.insightcentre.uld.naisc.blocking.Predefined;
import org.insightcentre.uld.naisc.constraint.Bijective;
import org.insightcentre.uld.naisc.constraint.Constraint;
import org.insightcentre.uld.naisc.constraint.ConstraintFactory;
import org.insightcentre.uld.naisc.constraint.ThresholdConstraint;
import org.insightcentre.uld.naisc.graph.PPR;
import org.insightcentre.uld.naisc.graph.PropertyOverlap;
import org.insightcentre.uld.naisc.lens.Label;
import org.insightcentre.uld.naisc.lens.LensAutoConfig;
import org.insightcentre.uld.naisc.lens.OntoLex;
import org.insightcentre.uld.naisc.lens.SPARQL;
import org.insightcentre.uld.naisc.lens.URI;
import org.insightcentre.uld.naisc.matcher.BeamSearch;
import org.insightcentre.uld.naisc.matcher.Greedy;
import org.insightcentre.uld.naisc.matcher.MonteCarloTreeSearch;
import org.insightcentre.uld.naisc.matcher.Threshold;
import org.insightcentre.uld.naisc.matcher.UniqueAssignment;
import org.insightcentre.uld.naisc.rescaling.Percentile;
import org.insightcentre.uld.naisc.scorer.Average;
import org.insightcentre.uld.naisc.scorer.LibSVM;
import org.insightcentre.uld.naisc.scorer.RAdLR;
import org.insightcentre.uld.naisc.util.Lazy;

/**
 * A configuration of the system
 *
 * @author John McCrae
 */
public class Configuration {

    /**
     * The configuration of the blocking strategies
     */
    public final BlockingStrategyConfiguration blocking;
    /**
     * The configuration of the lenses
     */
    public final List<LensConfiguration> lenses;
    /**
     * The configuration of the feature extractors
     */
    public final List<TextFeatureConfiguration> textFeatures;
    /**
     * The list of data features
     */
    public final List<GraphFeatureConfiguration> graphFeatures;
    /**
     * The configuration of the similarity classifier
     */
    public final List<ScorerConfiguration> scorers;
    /**
     * The configuration of the schema matcher
     */
    public final MatcherConfiguration matcher;
    /**
     * The rescaling method to use
     */
    public final RescalerMethod rescaler;
    /**
     * The description of the configuration
     */
    public final String description;
    /**
     * The number of threads to use
     */
    public int nThreads = 10;

    /**
     * Whether to include the features into the final output
     */
    public boolean includeFeatures = false;

    /**
     * Whether to ignore any elements that are already linked, otherwise new, alternative links will be suggested for these
     * elements
     */
    public boolean ignorePreexisting = false;

    /**
     * Do not force the matching of unique pairs; Match all elements.
     */
    public boolean noPrematching = false;

    @JsonCreator
    public Configuration(
            @JsonProperty("blocking") BlockingStrategyConfiguration blocking,
            @JsonProperty("lenses") List<LensConfiguration> lenses,
            @JsonProperty("graphFeatures") List<GraphFeatureConfiguration> dataFeatures,
            @JsonProperty("textFeatures") List<TextFeatureConfiguration> textFeatures,
            @JsonProperty("scorers") List<ScorerConfiguration> scorers,
            @JsonProperty("matcher") MatcherConfiguration matcher,
            @JsonProperty("description") String description,
            @JsonProperty("rescaler") RescalerMethod rescaler) {
        if (blocking == null) {
            throw new ConfigurationException("Blocking strategy not specified");
        }
        this.blocking = blocking;
        this.textFeatures = textFeatures == null ? Collections.EMPTY_LIST : textFeatures;
        this.graphFeatures = dataFeatures == null ? Collections.EMPTY_LIST : dataFeatures;
        if (this.textFeatures.isEmpty() && this.graphFeatures.isEmpty()) {
            throw new ConfigurationException("No features specified");
        }
        if (scorers == null || scorers.isEmpty()) {
            throw new ConfigurationException("No scorers specified");
        }
        this.scorers = scorers;
        if (matcher == null) {
            throw new ConfigurationException("No matcher specified");
        }
        this.matcher = matcher;
        this.lenses = lenses == null ? Collections.EMPTY_LIST : lenses;
        this.description = description;
        this.rescaler = rescaler;
    }

    public List<GraphFeature> makeGraphFeatures(Dataset model, Lazy<Analysis> analysis,
            AlignmentSet prelinking, NaiscListener listener) {
        List<GraphFeature> extractors = new ArrayList<>();
        for (GraphFeatureConfiguration config : graphFeatures) {
            GraphFeatureFactory extractor = Services.get(GraphFeatureFactory.class, config.name);
            extractors.add(extractor.makeFeature(model, config.params, analysis, prelinking, listener));
        }
        return extractors;

    }

    public List<TextFeature> makeTextFeatures() {
        List<TextFeature> extractors = new ArrayList<>();
        for (TextFeatureConfiguration config : textFeatures) {
            TextFeatureFactory extractor = Services.get(TextFeatureFactory.class, config.name);
            extractors.add(extractor.makeFeatureExtractor(config.tags, config.params));
        }
        if (extractors.isEmpty()) {
            System.err.println("No extractors loaded!");
        }
        return extractors;
    }

    public List<Lens> makeLenses(Dataset model, Lazy<Analysis> analysis, NaiscListener log) {
        List<Lens> ls = new ArrayList<>();
        if(lenses.isEmpty()) {
            return new LensAutoConfig().autoConfiguration(analysis.get(), model, log);
        }
        for (LensConfiguration config : lenses) {
            LensFactory lens = Services.get(LensFactory.class, config.name);
            ls.add(lens.makeLens(model, config.params));
        }
        if (ls.isEmpty()) {
            System.err.println("No lenses loaded!");
        }
        return ls;
    }

    public Scorer makeScorer() throws IOException {
        Scorer scorer = null;
        for (ScorerConfiguration config : this.scorers) {
            File path = config.modelFile == null ? null : new File(config.modelFile);
            if(scorer == null) {
                scorer = Services.get(ScorerFactory.class, config.name).makeScorer(config.params, path);
            } else {
                scorer = new MergedScorer(scorer, Services.get(ScorerFactory.class, config.name).makeScorer(config.params, path));
            }
        }
        return scorer;
    }

    public List<ScorerTrainer> makeTrainableScorers(String property, String tag) {
        List<ScorerTrainer> tsfs = new ArrayList<>();
        for (ScorerConfiguration config : this.scorers) {
            ScorerFactory sf = Services.get(ScorerFactory.class, config.name);
            final File path;
            if(tag != null) {
                path = config.modelFile == null ? null : new File(config.modelFile + tag);
                path.deleteOnExit();
            } else {
                path = config.modelFile == null ? null : new File(config.modelFile);
            }
            tsfs.addAll(sf.makeTrainer(config.params, property, path).toList());
        }
        if (tsfs.isEmpty()) {
            System.err.println("Training but no trainable scorers are in the configuration!");
        }
        return tsfs;
    }

    public Matcher makeMatcher() throws IOException {
        return Services.get(MatcherFactory.class, this.matcher.name).makeMatcher(this.matcher.params);
    }

    public BlockingStrategy makeBlockingStrategy(Lazy<Analysis> analysis, NaiscListener listener) throws IOException {
        return Services.get(BlockingStrategyFactory.class, this.blocking.name).makeBlockingStrategy(this.blocking.params, analysis, listener);
    }

    public static Class[] knownTextFeatures = new Class[]{
        BagOfWordsSim.class,
        BasicString.class,
        Dictionary.class,
        KeyWords.class,
        WordEmbeddings.class,
        WordNet.class,
        org.insightcentre.uld.naisc.feature.Command.class,
            MachineTranslation.class
    };

    public Rescaler makeRescaler() {
        if(rescaler == null || rescaler == RescalerMethod.NoScaling) {
            return new NoRescaling();
        } else if(rescaler == RescalerMethod.Percentile) {
            return new Percentile();
        } else if(rescaler == RescalerMethod.MinMax) {
            return new MinMax();
        } else {
            throw new ConfigurationException(rescaler + " is not a valid value for the rescaler");
        }
    }

    /**
     * The configuration for text feature extractors. The following extractors
     * are implemented in the basic Naisc tool:
     *
     * <ul>
     * <li>Basic String metrics
     * {@link org.insightcentre.uld.naisc.feature.BasicString.Configuration}</li>
     * <li>Bag of Words Similarity
     * {@link org.insightcentre.uld.naisc.feature.BagOfWordsSim.Configuration}</li>
     * <li>Dictionary matching
     * {@link org.insightcentre.uld.naisc.feature.Dictionary.Configuration}</li>
     * <li>Keyword matching
     * {@link org.insightcentre.uld.naisc.feature.KeyWords.Configuration}</li>
     * <li>WordNet-based similarity
     * {@link org.insightcentre.uld.naisc.feature.WordNet.Configuration}</li>
     * <li>Word embedding similarity
     * {@link org.insightcentre.uld.naisc.feature.WordEmbeddings.Configuration}</li>
     * </ul>
     */
    @JsonDeserialize(using = TextFeatureConfigurationDeserializer.class)
    @JsonSerialize(using = TextFeatureConfigurationSerializer.class)
    public static class TextFeatureConfiguration {

        /**
         * The name of the class that implements this feature extractor. For
         * core feature extractors the prefix
         * <code>org.insightcentre.uld.naisc.</code> may be omitted
         */
        public final String name;
        /**
         * The parameters to configure the feature extractor.
         */
        public final Map<String, Object> params;
        /**
         * The tags (of lenses) to apply this feature extractor to
         */
        public final Set<String> tags;

        @JsonCreator
        public TextFeatureConfiguration(@JsonProperty("name") String name,
                @JsonProperty("params") Map<String, Object> params,
                @JsonProperty("tags") Set<String> tags) {
            this.name = name;
            this.params = params == null ? Collections.EMPTY_MAP : params;
            this.tags = tags == null || tags.isEmpty() ? null : tags;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + Objects.hashCode(this.name);
            hash = 97 * hash + Objects.hashCode(this.params);
            hash = 97 * hash + Objects.hashCode(this.tags);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TextFeatureConfiguration other = (TextFeatureConfiguration) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.params, other.params)) {
                return false;
            }
            if (!Objects.equals(this.tags, other.tags)) {
                return false;
            }
            return true;
        }

    }

    private static class TextFeatureConfigurationDeserializer extends StdDeserializer<TextFeatureConfiguration> {

        public TextFeatureConfigurationDeserializer() {
            super(TextFeatureConfiguration.class);
        }

        @Override
        public TextFeatureConfiguration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            String name = null;
            Map<String, Object> params = new HashMap<>();
            Set<String> tags = null;
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                if (f.getKey().equals("name")) {
                    name = f.getValue().textValue();
                } else if (f.getKey().equals("tags")) {
                    tags = new HashSet<>();
                    final Iterator<JsonNode> elements = f.getValue().elements();
                    while (elements.hasNext()) {
                        tags.add(elements.next().asText());
                    }
                } else {
                    // HACK: This is working around what I assume is a bug in Jackson
                    // UPDATE: Actually a UI bug that is now fixed but this code does no harm :)
                    Object v = ((ObjectMapper)p.getCodec()).convertValue(f.getValue(), Object.class);
                    if(v instanceof List) {
                        ListIterator<Object> i = ((List)v).listIterator();
                        while(i.hasNext()) {
                            Object n = i.next();
                            if(n instanceof Map && ((Map)n).containsKey("0")) {
                                StringBuilder sb = new StringBuilder();
                                for(int j = 0; ; j++) {
                                    if(!((Map)n).containsKey(j + ""))
                                        break;
                                    sb.append(((Map)n).get(j + "").toString());
                                }
                                i.set(sb.toString());
                            }
                            
                        }
                    }
                    params.put(f.getKey(), v);
                    
                            
                }
            }
            return new TextFeatureConfiguration(name, params, tags);
        }
    }

    private static class TextFeatureConfigurationSerializer extends StdSerializer<TextFeatureConfiguration> {

        public TextFeatureConfigurationSerializer() {
            super(TextFeatureConfiguration.class);
        }

        @Override
        public void serialize(TextFeatureConfiguration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name);
            if (value.tags != null) {
                gen.writeArrayFieldStart("tags");
                for (String t : value.tags) {
                    gen.writeString(t);
                }
                gen.writeEndArray();
            }
            for (Map.Entry<String, Object> e : value.params.entrySet()) {
                gen.writeObjectField(e.getKey(), e.getValue());
            }
            gen.writeEndObject();
        }
    }

    public static Class[] knownGraphFeatures = new Class[]{
        PropertyOverlap.class,
        org.insightcentre.uld.naisc.graph.Command.class,
        Automatic.class,
        PPR.class
    };

    /**
     * The configuration for graph feature extractors. The following extractors
     * are implemented in the basic Naisc tool:
     *
     * <ul>
     * <li>Property Overlap
     * {@link org.insightcentre.uld.naisc.graph.PropertyOverlap.Configuration}</li>
     * </ul>
     */
    @JsonDeserialize(using = GraphFeatureConfigurationDeserializer.class)
    @JsonSerialize(using = GraphFeatureConfigurationSerializer.class)
    public static class GraphFeatureConfiguration {

        /**
         * The name of the class that implements this feature extractor. For
         * core feature extractors the prefix
         * <code>org.insightcentre.uld.naisc.</code> may be omitted
         */
        public final String name;
        /**
         * The parameters to configure the feature extractor.
         */
        public final Map<String, Object> params;
        /**
         * The tags (of lenses) to apply this feature extractor to
         */
        public final Set<String> tags;

        @JsonCreator
        public GraphFeatureConfiguration(@JsonProperty("name") String name,
                @JsonProperty("params") Map<String, Object> params,
                @JsonProperty("tags") Set<String> tags) {
            this.name = name;
            this.params = params == null ? Collections.EMPTY_MAP : params;
            this.tags = tags;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + Objects.hashCode(this.name);
            hash = 53 * hash + Objects.hashCode(this.params);
            hash = 53 * hash + Objects.hashCode(this.tags);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GraphFeatureConfiguration other = (GraphFeatureConfiguration) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.params, other.params)) {
                return false;
            }
            if (!Objects.equals(this.tags, other.tags)) {
                return false;
            }
            return true;
        }

    }

    private static class GraphFeatureConfigurationDeserializer extends StdDeserializer<GraphFeatureConfiguration> {

        public GraphFeatureConfigurationDeserializer() {
            super(GraphFeatureConfiguration.class);
        }

        @Override
        public GraphFeatureConfiguration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            String name = null;
            Map<String, Object> params = new HashMap<>();
            Set<String> tags = null;
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                if (f.getKey().equals("name")) {
                    name = f.getValue().textValue();
                } else if (f.getKey().equals("tags")) {
                    tags = new HashSet<>();
                    final Iterator<JsonNode> elements = f.getValue().elements();
                    while (elements.hasNext()) {
                        tags.add(elements.next().asText());
                    }
                } else {
                    params.put(f.getKey(), p.getCodec().readValue(f.getValue().traverse(), Object.class));
                }
            }
            return new GraphFeatureConfiguration(name, params, tags);
        }
    }

    private static class GraphFeatureConfigurationSerializer extends StdSerializer<GraphFeatureConfiguration> {

        public GraphFeatureConfigurationSerializer() {
            super(GraphFeatureConfiguration.class);
        }

        @Override
        public void serialize(GraphFeatureConfiguration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name);
            if (value.tags != null) {
                gen.writeArrayFieldStart("tags");
                for (String t : value.tags) {
                    gen.writeString(t);
                }
                gen.writeEndArray();
            }
            for (Map.Entry<String, Object> e : value.params.entrySet()) {
                gen.writeObjectField(e.getKey(), e.getValue());
            }
            gen.writeEndObject();
        }
    }

    public static Class[] knownScorers = new Class[]{
        Average.class,
        LibSVM.class,
        org.insightcentre.uld.naisc.scorer.Command.class,
        RAdLR.class
    };

    /**
     * The configuration for a scorer. The following scorers are implemented in
     * the basic Naisc tool:
     *
     * <ul>
     * <li>Average (unsupervised)
     * {@link org.insightcentre.uld.naisc.scorer.Average.Configuration}</li>
     * <li>LibSVM (supervised)
     * {@link org.insightcentre.uld.naisc.scorer.LibSVM.Configuration}</li>
     * </ul>
     */
    @JsonDeserialize(using = ScorerConfigurationDeserializer.class)
    @JsonSerialize(using = ScorerConfigurationSerializer.class)
    public static class ScorerConfiguration {

        /**
         * The name of the class that implements this scorer. For core scorers
         * the prefix <code>org.insightcentre.uld.naisc.</code> may be omitted
         */
        public final String name;
        /**
         * The parameters to configure the scorer.
         */
        public final Map<String, Object> params;
        /**
         * The path to the model file.
         */
        public final String modelFile;

        @JsonCreator
        public ScorerConfiguration(
                @JsonProperty("name") String name,
                @JsonProperty("params") Map<String, Object> params,
                @JsonProperty("modelFile") String modelFile) {
            this.name = name;
            assert (name != null);
            this.params = params == null ? Collections.EMPTY_MAP : params;
            this.modelFile = modelFile;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.name);
            hash = 97 * hash + Objects.hashCode(this.params);
            hash = 97 * hash + Objects.hashCode(this.modelFile);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ScorerConfiguration other = (ScorerConfiguration) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.modelFile, other.modelFile)) {
                return false;
            }
            if (!Objects.equals(this.params, other.params)) {
                return false;
            }
            return true;
        }


    }

    private static class ScorerConfigurationDeserializer extends StdDeserializer<ScorerConfiguration> {

        public ScorerConfigurationDeserializer() {
            super(ScorerConfiguration.class);
        }

        @Override
        public ScorerConfiguration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            String name = null;
            String modelFile = null;
            Map<String, Object> params = new HashMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                if (f.getKey().equals("name")) {
                    name = f.getValue().textValue();
                } else if(f.getKey().equals("modelFile")) {
                    modelFile = f.getValue().textValue();
                } else {
                    params.put(f.getKey(), p.getCodec().readValue(f.getValue().traverse(), Object.class));
                }
            }
            return new ScorerConfiguration(name, params, modelFile);
        }
    }

    private static class ScorerConfigurationSerializer extends StdSerializer<ScorerConfiguration> {

        public ScorerConfigurationSerializer() {
            super(ScorerConfiguration.class);
        }

        @Override
        public void serialize(ScorerConfiguration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name);
            gen.writeStringField("modelFile", value.modelFile);
            for (Map.Entry<String, Object> e : value.params.entrySet()) {
                gen.writeObjectField(e.getKey(), e.getValue());
            }
            gen.writeEndObject();
        }
    }

    public static Class[] knownMatchers = new Class[]{
        Threshold.class,
        UniqueAssignment.class,
        Greedy.class,
        BeamSearch.class,
        org.insightcentre.uld.naisc.matcher.Command.class,
        MonteCarloTreeSearch.class
    };

    /**
     * The configuration for a matcher. The following scorers are implemented in
     * the basic Naisc tool:
     *
     * <ul>
     * <li>Threshold-based
     * {@link org.insightcentre.uld.naisc.matcher.Threshold.Configuration}</li>
     * <li>Unique (bijective) assignment
     * {@link org.insightcentre.uld.naisc.matcher.UniqueAssignment.Configuration}</li>
     * <li>Greedy solver
     * {@link org.insightcentre.uld.naisc.matcher.Greedy.Configuration}</li>
     * <li>Beam searcher
     * {@link org.insightcentre.uld.naisc.matcher.BeamSearch.Configuration}</li>
     * </ul>
     */
    @JsonDeserialize(using = MatcherConfigurationDeserializer.class)
    @JsonSerialize(using = MatcherConfigurationSerializer.class)
    public static class MatcherConfiguration {

        /**
         * The name of the class that implements this matcher. For core matchers
         * the prefix <code>org.insightcentre.uld.naisc.</code> may be omitted
         */
        public final String name;
        /**
         * The parameters to configure the matcher.
         */
        public final Map<String, Object> params;

        @JsonCreator
        public MatcherConfiguration(
                @JsonProperty("name") String name,
                @JsonProperty("params") Map<String, Object> params) {
            this.name = name;
            assert (name != null);
            this.params = params == null ? Collections.EMPTY_MAP : params;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.name);
            hash = 97 * hash + Objects.hashCode(this.params);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MatcherConfiguration other = (MatcherConfiguration) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.params, other.params)) {
                return false;
            }
            return true;
        }

    }

    private static class MatcherConfigurationDeserializer extends StdDeserializer<MatcherConfiguration> {

        public MatcherConfigurationDeserializer() {
            super(MatcherConfiguration.class);
        }

        @Override
        public MatcherConfiguration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            String name = null;
            Map<String, Object> params = new HashMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                if (f.getKey().equals("name")) {
                    name = f.getValue().textValue();
                } else {
                    params.put(f.getKey(), p.getCodec().readValue(f.getValue().traverse(), Object.class));
                }
            }
            return new MatcherConfiguration(name, params);
        }
    }

    private static class MatcherConfigurationSerializer extends StdSerializer<MatcherConfiguration> {

        public MatcherConfigurationSerializer() {
            super(MatcherConfiguration.class);
        }

        @Override
        public void serialize(MatcherConfiguration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name);
            for (Map.Entry<String, Object> e : value.params.entrySet()) {
                gen.writeObjectField(e.getKey(), e.getValue());
            }
            gen.writeEndObject();
        }
    }

    public static Class[] knownLenses = new Class[]{
        Label.class,
        URI.class,
        SPARQL.class,
        OntoLex.class,
        org.insightcentre.uld.naisc.lens.Command.class
    };

    /**
     * The configuration for a lens. The following lenses are implemented in the
     * basic Naisc tool:
     *
     * <ul>
     * <li>Label
     * {@link org.insightcentre.uld.naisc.lens.Label.Configuration}</li>
     * <li>From URI
     * {@link org.insightcentre.uld.naisc.lens.URI.Configuration}</li>
     * <li>By SPARQL query
     * {@link org.insightcentre.uld.naisc.lens.SPARQL.Configuration}</li>
     * </ul>
     */
    @JsonDeserialize(using = LensConfigurationDeserializer.class)
    @JsonSerialize(using = LensConfigurationSerializer.class)
    public static class LensConfiguration {

        /**
         * The name of the class that implements this lens. For core lenses the
         * prefix <code>org.insightcentre.uld.naisc.</code> may be omitted
         */
        public final String name;
        /**
         * The parameters to configure the lens.
         */
        public final Map<String, Object> params;

        @JsonCreator
        public LensConfiguration(
                @JsonProperty("name") String name,
                @JsonProperty("params") Map<String, Object> params) {
            this.name = name;
            assert (name != null);
            this.params = params == null ? Collections.EMPTY_MAP : params;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + Objects.hashCode(this.name);
            hash = 53 * hash + Objects.hashCode(this.params);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LensConfiguration other = (LensConfiguration) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.params, other.params)) {
                return false;
            }
            return true;
        }

    }

    private static class LensConfigurationDeserializer extends StdDeserializer<LensConfiguration> {

        public LensConfigurationDeserializer() {
            super(LensConfiguration.class);
        }

        @Override
        public LensConfiguration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            String name = null;
            String tag = null;
            Map<String, Object> params = new HashMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                if (f.getKey().equals("name")) {
                    name = f.getValue().textValue();
                } else {
                    params.put(f.getKey(), p.getCodec().readValue(f.getValue().traverse(), Object.class));
                }
            }
            return new LensConfiguration(name, params);
        }
    }

    private static class LensConfigurationSerializer extends StdSerializer<LensConfiguration> {

        public LensConfigurationSerializer() {
            super(LensConfiguration.class);
        }

        @Override
        public void serialize(LensConfiguration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name);
            for (Map.Entry<String, Object> e : value.params.entrySet()) {
                gen.writeObjectField(e.getKey(), e.getValue());
            }
            gen.writeEndObject();
        }
    }

    public static Class[] knownBlockingStrategies = new Class[]{
        org.insightcentre.uld.naisc.blocking.Automatic.class,
        All.class,
        IDMatch.class,
        LabelMatch.class,
        ApproximateStringMatching.class,
        Predefined.class,
        org.insightcentre.uld.naisc.blocking.OntoLex.class,
        org.insightcentre.uld.naisc.blocking.Command.class,
        Path.class
    };

    /**
     * The configuration for a blocking strategy. The following strategies are
     * implemented in the basic Naisc tool:
     *
     * <ul>
     * <li>All pairs (no blocking)
     * {@link org.insightcentre.uld.naisc.blocking.All.Configuration}</li>
     * <li>Matching IDs
     * {@link org.insightcentre.uld.naisc.blocking.IDMatch.Configuration}</li>
     * <li>Matching Labels
     * {@link org.insightcentre.uld.naisc.blocking.LabelMatch.Configuration}</li>
     * </ul>
     */
    @JsonDeserialize(using = BlockingStrategyConfigurationDeserializer.class)
    @JsonSerialize(using = BlockingStrategyConfigurationSerializer.class)
    public static class BlockingStrategyConfiguration {

        /**
         * The name of the class that implements this strategy. For core
         * strategies the prefix <code>org.insightcentre.uld.naisc.</code> may
         * be omitted
         */
        public final String name;
        /**
         * The parameters to configure the strategy.
         */
        public final Map<String, Object> params;

        @JsonCreator
        public BlockingStrategyConfiguration(
                @JsonProperty("name") String name,
                @JsonProperty("params") Map<String, Object> params) {
            this.name = name;
            assert (name != null);
            this.params = params == null ? Collections.EMPTY_MAP : params;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.name);
            hash = 41 * hash + Objects.hashCode(this.params);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final BlockingStrategyConfiguration other = (BlockingStrategyConfiguration) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.params, other.params)) {
                return false;
            }
            return true;
        }

    }

    private static class BlockingStrategyConfigurationDeserializer extends StdDeserializer<BlockingStrategyConfiguration> {

        public BlockingStrategyConfigurationDeserializer() {
            super(BlockingStrategyConfiguration.class);
        }

        @Override
        public BlockingStrategyConfiguration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            String name = null;
            Map<String, Object> params = new HashMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                if (f.getKey().equals("name")) {
                    name = f.getValue().textValue();
                } else {
                    params.put(f.getKey(), p.getCodec().readValue(f.getValue().traverse(), Object.class));
                }
            }
            return new BlockingStrategyConfiguration(name, params);
        }
    }

    private static class BlockingStrategyConfigurationSerializer extends StdSerializer<BlockingStrategyConfiguration> {

        public BlockingStrategyConfigurationSerializer() {
            super(BlockingStrategyConfiguration.class);
        }

        @Override
        public void serialize(BlockingStrategyConfiguration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name);
            for (Map.Entry<String, Object> e : value.params.entrySet()) {
                gen.writeObjectField(e.getKey(), e.getValue());
            }
            gen.writeEndObject();
        }
    }

    public static Class[] knownConstraints = new Class[]{
        ThresholdConstraint.class,
        Bijective.class

    };

    /**
     * The configuration of a constraint. The following constraints are provided
     * by Naisc:
     * <ul>
     * <li>Simple Threshold constraint (can be used as non constraint)
     * {@link org.insightcentre.uld.naisc.constraint.ThresholdConstraint.Configuration}</li>
     * <li>Bijective constraint (can be used as non constraint)
     * {@link org.insightcentre.uld.naisc.constraint.Bijective.Configuration}</li>
     * </ul>
     *
     * @author John McCrae
     */
    @JsonDeserialize(using = ConstraintConfigurationDeserializer.class)
    @JsonSerialize(using = ConstraintConfigurationSerializer.class)
    public static class ConstraintConfiguration {

        public final String name;
        public final Map<String, Object> params;

        public ConstraintConfiguration(@JsonProperty(value = "name") String name, @JsonProperty(value = "params") Map<String, Object> params) {
            this.name = name;
            this.params = params;
        }

        public Constraint make() {
            if (name == null) {
                //return new EdgeCorrectness().make(Collections.EMPTY_MAP);
                throw new IllegalArgumentException("Name cannot be null");
            } else {
                return Services.get(ConstraintFactory.class, name).make(params == null ? Collections.EMPTY_MAP : params);
            }
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + Objects.hashCode(this.name);
            hash = 79 * hash + Objects.hashCode(this.params);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ConstraintConfiguration other = (ConstraintConfiguration) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.params, other.params)) {
                return false;
            }
            return true;
        }

    }

    private static class ConstraintConfigurationDeserializer extends StdDeserializer<ConstraintConfiguration> {

        public ConstraintConfigurationDeserializer() {
            super(ConstraintConfiguration.class);
        }

        @Override
        public ConstraintConfiguration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            String name = null;
            Map<String, Object> params = new HashMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                if (f.getKey().equals("name")) {
                    name = f.getValue().textValue();
                } else {
                    params.put(f.getKey(), p.getCodec().readValue(f.getValue().traverse(), Object.class));
                }
            }
            return new ConstraintConfiguration(name, params);
        }
    }

    private static class ConstraintConfigurationSerializer extends StdSerializer<ConstraintConfiguration> {

        public ConstraintConfigurationSerializer() {
            super(ConstraintConfiguration.class);
        }

        @Override
        public void serialize(ConstraintConfiguration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name);
            for (Map.Entry<String, Object> e : value.params.entrySet()) {
                gen.writeObjectField(e.getKey(), e.getValue());
            }
            gen.writeEndObject();
        }
    }

    /**
     * Values for the rescaler
     */
     public static enum RescalerMethod {
        NoScaling,
        MinMax,
        Percentile
    }


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.blocking);
        hash = 43 * hash + Objects.hashCode(this.lenses);
        hash = 43 * hash + Objects.hashCode(this.textFeatures);
        hash = 43 * hash + Objects.hashCode(this.graphFeatures);
        hash = 43 * hash + Objects.hashCode(this.scorers);
        hash = 43 * hash + Objects.hashCode(this.matcher);
        hash = 43 * hash + Objects.hashCode(this.description);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Configuration other = (Configuration) obj;
        if (!Objects.equals(this.description, other.description)) {
            return false;
        }
        if (!Objects.equals(this.blocking, other.blocking)) {
            return false;
        }
        if (!Objects.equals(this.lenses, other.lenses)) {
            return false;
        }
        if (!Objects.equals(this.textFeatures, other.textFeatures)) {
            return false;
        }
        if (!Objects.equals(this.graphFeatures, other.graphFeatures)) {
            return false;
        }
        if (!Objects.equals(this.scorers, other.scorers)) {
            return false;
        }
        if (!Objects.equals(this.matcher, other.matcher)) {
            return false;
        }
        return true;
    }
    
    
}
