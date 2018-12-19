package org.insightcentre.uld.naisc.main;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.BlockingStrategyFactory;
import org.insightcentre.uld.naisc.Lens;
import org.insightcentre.uld.naisc.LensFactory;
import org.insightcentre.uld.naisc.Matcher;
import org.insightcentre.uld.naisc.MatcherFactory;
import org.insightcentre.uld.naisc.Scorer;
import org.insightcentre.uld.naisc.ScorerFactory;
import org.insightcentre.uld.naisc.util.Services;
import org.insightcentre.uld.naisc.ScorerTrainer;
import org.insightcentre.uld.naisc.TextFeature;
import org.insightcentre.uld.naisc.TextFeatureFactory;
import org.insightcentre.uld.naisc.GraphFeature;
import org.insightcentre.uld.naisc.GraphFeatureFactory;
import org.insightcentre.uld.naisc.blocking.All;
import org.insightcentre.uld.naisc.blocking.ApproximateStringMatching;
import org.insightcentre.uld.naisc.blocking.IDMatch;
import org.insightcentre.uld.naisc.blocking.LabelMatch;
import org.insightcentre.uld.naisc.constraint.Bijective;
import org.insightcentre.uld.naisc.constraint.Constraint;
import org.insightcentre.uld.naisc.constraint.ConstraintFactory;
import org.insightcentre.uld.naisc.constraint.ThresholdConstraint;
import org.insightcentre.uld.naisc.feature.BagOfWordsSim;
import org.insightcentre.uld.naisc.feature.BasicString;
import org.insightcentre.uld.naisc.feature.Dictionary;
import org.insightcentre.uld.naisc.feature.KeyWords;
import org.insightcentre.uld.naisc.feature.WordEmbeddings;
import org.insightcentre.uld.naisc.feature.WordNet;
import org.insightcentre.uld.naisc.graph.PropertyOverlap;
import org.insightcentre.uld.naisc.lens.Label;
import org.insightcentre.uld.naisc.lens.SPARQL;
import org.insightcentre.uld.naisc.lens.URI;
import org.insightcentre.uld.naisc.matcher.BeamSearch;
import org.insightcentre.uld.naisc.matcher.Greedy;
import org.insightcentre.uld.naisc.matcher.Threshold;
import org.insightcentre.uld.naisc.matcher.UniqueAssignment;
import org.insightcentre.uld.naisc.scorer.Average;
import org.insightcentre.uld.naisc.scorer.LibSVM;

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
    public final List<GraphFeatureConfiguration> dataFeatures;
    /**
     * The configuration of the similarity classifier
     */
    public final List<ScorerConfiguration> scorers;
    /**
     * The configuration of the schema matcher
     */
    public final MatcherConfiguration matcher;

    @JsonCreator
    public Configuration(
            @JsonProperty("blocking") BlockingStrategyConfiguration blocking,
            @JsonProperty("lenses") List<LensConfiguration> lenses,
            @JsonProperty("dataFeatures") List<GraphFeatureConfiguration> dataFeatures,
            @JsonProperty("textFeatures") List<TextFeatureConfiguration> textFeatures,
            @JsonProperty("scorers") List<ScorerConfiguration> scorers,
            @JsonProperty("matcher") MatcherConfiguration matcher) {
        if (blocking == null) {
            throw new ConfigurationException("Blocking strategy not specified");
        }
        this.blocking = blocking;
        this.textFeatures = textFeatures == null ? Collections.EMPTY_LIST : textFeatures;
        this.dataFeatures = dataFeatures == null ? Collections.EMPTY_LIST : dataFeatures;
        if (this.textFeatures.isEmpty() && this.dataFeatures.isEmpty()) {
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
    }

    public List<GraphFeature> makeDataFeatures(Model model) {
        List<GraphFeature> extractors = new ArrayList<>();
        for (GraphFeatureConfiguration config : dataFeatures) {
            GraphFeatureFactory extractor = Services.get(GraphFeatureFactory.class, config.name);
            extractors.add(extractor.makeFeature(model, config.params));
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

    public List<Lens> makeLenses(Model model) {
        List<Lens> ls = new ArrayList<>();
        for (LensConfiguration config : lenses) {
            LensFactory lens = Services.get(LensFactory.class, config.name);
            ls.add(lens.makeLens(config.tag, model, config.params));
        }
        if (ls.isEmpty()) {
            System.err.println("No lenses loaded!");
        }
        return ls;
    }

    public List<Scorer> makeScorer() throws IOException {
        List<Scorer> scorerList = new ArrayList<>();
        for (ScorerConfiguration config : this.scorers) {
            scorerList.add(Services.get(ScorerFactory.class, config.name).makeScorer(config.params));
        }
        if (scorerList.isEmpty()) {
            System.err.println("No scorers loaded!");
        }
        return scorerList;
    }

    public List<ScorerTrainer> makeTrainableScorers() {
        List<ScorerTrainer> tsfs = new ArrayList<>();
        for (ScorerConfiguration config : this.scorers) {
            ScorerFactory sf = Services.get(ScorerFactory.class, config.name);
            tsfs.addAll(sf.makeTrainer(config.params).toList());
        }
        if (tsfs.isEmpty()) {
            System.err.println("No trainable scorers loaded!");
        }
        return tsfs;
    }

    public Matcher makeMatcher() throws IOException {
        return Services.get(MatcherFactory.class, this.matcher.name).makeMatcher(this.matcher.params);
    }

    public BlockingStrategy makeBlockingStrategy() throws IOException {
        return Services.get(BlockingStrategyFactory.class, this.blocking.name).makeBlockingStrategy(this.blocking.params);
    }

    public static Class[] knownTextFeatures = new Class[]{
        BagOfWordsSim.class,
        BasicString.class,
        Dictionary.class,
        KeyWords.class,
        WordEmbeddings.class,
        WordNet.class
    };

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
            this.tags = tags;
        }
    }

    public static Class[] knownGraphFeatures = new Class[]{
        PropertyOverlap.class
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
    }

    public static Class[] knownScorers = new Class[]{
        Average.class,
        LibSVM.class
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

        @JsonCreator
        public ScorerConfiguration(
                @JsonProperty("name") String name,
                @JsonProperty("train") boolean train,
                @JsonProperty("params") Map<String, Object> params) {
            this.name = name;
            assert (name != null);
            this.params = params == null ? Collections.EMPTY_MAP : params;
        }
    }

    public static Class[] knownMatchers = new Class[]{
        Threshold.class,
        UniqueAssignment.class,
        Greedy.class,
        BeamSearch.class
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
    }

    public static Class[] knownLenses = new Class[]{
        Label.class,
        URI.class,
        SPARQL.class
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
        /**
         * The tag associated with this lens (null for no tag)
         */
        public final String tag;

        @JsonCreator
        public LensConfiguration(
                @JsonProperty("name") String name,
                @JsonProperty("params") Map<String, Object> params,
                @JsonProperty("tag") String tag) {
            this.name = name;
            assert (name != null);
            this.params = params == null ? Collections.EMPTY_MAP : params;
            this.tag = tag;
        }
    }

    public static Class[] knownBlockingStrategies = new Class[]{
        All.class,
        IDMatch.class,
        LabelMatch.class,
        ApproximateStringMatching.class
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
    }

    public static Class[] knownConstraints = new Class[] {
        ThresholdConstraint.class,
        Bijective.class
        
    };
    
    /**
     * The configuration of a constraint. The following constraints are provided
     * by Naisc:
     * <ul>
     * <li>Simple Threshold constraint (can be used as non constraint) {@link org.insightcentre.uld.naisc.constraint.ThresholdConstraint.Configuration}</li>
     * <li>Bijective constraint (can be used as non constraint) {@link org.insightcentre.uld.naisc.constraint.Bijective.Configuration}</li>
     * </ul>
     * @author John McCrae
     */
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

    }
}
