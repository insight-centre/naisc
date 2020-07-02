package org.insightcentre.uld.naisc.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.jena.rdf.model.ModelFactory
import org.insightcentre.uld.naisc.*
import org.insightcentre.uld.naisc.analysis.Analysis
import org.insightcentre.uld.naisc.analysis.DatasetAnalyzer
import org.insightcentre.uld.naisc.main.CombinedDataset
import org.insightcentre.uld.naisc.main.Configuration
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader
import org.insightcentre.uld.naisc.main.ExecuteListeners
import org.insightcentre.uld.naisc.util.Lazy
import org.insightcentre.uld.naisc.util.Pair
import java.io.File
import java.io.FileReader


import java.io.IOException
import java.io.StringReader

object ConfigurationManager {

    private val configurations = mutableMapOf<String, Configuration>()
    private val datasets = mutableMapOf<String, Dataset>()
    private val analyses = mutableMapOf<Pair<String, String>, Lazy<Analysis>>()
    private val prelinking = mutableMapOf<Pair<String, String>, Lazy<AlignmentSet>>()
    private val strategies = mutableMapOf<ConfigDataset, BlockingStrategy>()
    private val lenses = mutableMapOf<ConfigDataset, List<Lens>>()
    private val graphFeatures = mutableMapOf<ConfigDataset, List<GraphFeature>>()
    private val textFeatures = mutableMapOf<String, List<TextFeature>>()
    private val scorers = mutableMapOf<String, Scorer>()
    private val matchers = mutableMapOf<String, Matcher>()

    @Throws(InvalidConfigurationException::class)
    private fun loadConfiguration(configuration: String): Configuration {
        if (configurations[configuration] != null) {
            return configurations[configuration]!!
        }
        val mapper = ObjectMapper()
        try {
            val f = File("configs/$configuration.json")
            if(!f.exists()) {
                throw InvalidConfigurationException("No such configuration \"$configuration\" (looking at ${f.absolutePath})")
            }
            val config = mapper.readValue<Configuration>(FileReader("configs/$configuration.json"), Configuration::class.java)
            configurations[configuration] = config
            return config
        } catch (x: Exception) {
            throw InvalidConfigurationException(x.message ?: "Unknown configuration error")
        }

    }

    @Throws(DatasetNotFoundException::class)
    fun getDataset(dataset: String): Dataset {
        return datasets.getOrElse(dataset, { throw DatasetNotFoundException("$dataset has not been uploaded") })
    }

    fun loadDataset(dataset : String, content : String) : Dataset {
        val model = ModelFactory.createDefaultModel()
        model.read(StringReader(content), "file:$dataset/")
        val modelDataset = DefaultDatasetLoader.ModelDataset(model, dataset)
        datasets[dataset] = modelDataset
        return modelDataset
    }

    fun getAnalysis(dataset1 : Dataset, dataset2 : Dataset) : Lazy<Analysis> {
        return analyses[Pair(dataset1.id(), dataset2.id())]?:(object : Lazy<Analysis>() {
                override fun init(): Analysis {
                    val analyzer = DatasetAnalyzer()
                    return analyzer.analyseModel(dataset1, dataset2)
                }
            })
    }

    fun getPrelinking(dataset1: Dataset, dataset2 : Dataset) : AlignmentSet  {
        // TODO: Figure out how pre-linking should really work
        return AlignmentSet()
    }

    @Throws(InvalidConfigurationException::class, DatasetNotFoundException::class, IOException::class)
    fun getStrategy(configuration: String, dataset1: String, dataset2: String): BlockingStrategy {
        val strategy = strategies.get(ConfigDataset(configuration, dataset1, dataset2))
        if(strategy != null) {
            return strategy
        }
        val left = getDataset(dataset1)
        val right = getDataset(dataset2)
        val strategy2 = loadConfiguration(configuration).makeBlockingStrategy(getAnalysis(left, right), ExecuteListeners.STDERR)
        strategies[ConfigDataset(configuration, dataset1, dataset2)] = strategy2
        return strategy2
    }

    @Throws(InvalidConfigurationException::class, DatasetNotFoundException::class)
    fun getLens(configuration: String, dataset1: String, dataset2: String): List<Lens> {
        val cacheLens = lenses[ConfigDataset(configuration, dataset1, dataset2)]
        if (cacheLens != null) {
            return cacheLens
        }
        val left = getDataset(dataset1)
        val right = getDataset(dataset2)
        val combined = CombinedDataset(left, right)
        val lens = loadConfiguration(configuration).makeLenses(combined, getAnalysis(left, right), ExecuteListeners.STDERR)
        lenses[ConfigDataset(configuration, dataset1, dataset2)] = lens
        return lens
    }

    @Throws(InvalidConfigurationException::class, DatasetNotFoundException::class)
    fun getGraphFeatures(configuration: String, dataset1 : String, dataset2 : String) : List<GraphFeature> {
        val cacheFeatures = graphFeatures.get(ConfigDataset(configuration, dataset1, dataset2))
        if (cacheFeatures != null) {
            return cacheFeatures
        }
        val left = getDataset(dataset1)
        val right = getDataset(dataset2)
        val combined = CombinedDataset(left, right)
        val features = loadConfiguration(configuration).makeGraphFeatures(combined, getAnalysis(left, right), getPrelinking(left, right), ExecuteListeners.STDERR)
        graphFeatures[ConfigDataset(configuration, dataset1, dataset2)] = features
        return features
    }

    @Throws(InvalidConfigurationException::class)
    fun getMatcher(configuration : String) : Matcher {
        val cacheMatcher = matchers.get(configuration)
        if (cacheMatcher != null) {
            return cacheMatcher
        }
        val matcher = loadConfiguration(configuration).makeMatcher()
        matchers[configuration] = matcher
        return matcher
    }

    @Throws(InvalidConfigurationException::class)
    fun getScorer(configuration: String) : Scorer {
        val cacheScorer = scorers.get(configuration)
        if (cacheScorer != null) {
            return cacheScorer
        }
        val scorer = loadConfiguration(configuration).makeScorer()
        scorers[configuration] = scorer
        return scorer
    }

    @Throws(InvalidConfigurationException::class)
    fun getTextFeatures(configuration : String) : List<TextFeature> {
        val cacheFeatures = textFeatures.get(configuration)
        if (cacheFeatures != null) {
            return cacheFeatures
        }
        val features = loadConfiguration(configuration).makeTextFeatures()
        textFeatures[configuration] = features
        return features
    }

}

data class ConfigDataset(val configuration : String, val dataset1 : String, val dataset2 : String)
