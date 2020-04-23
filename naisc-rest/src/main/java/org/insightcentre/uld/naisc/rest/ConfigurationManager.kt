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

    fun getPrelinking(dataset1: Dataset, dataset2 : Dataset) : Lazy<AlignmentSet>  {
        // TODO: Figure out how pre-linking should really work
        System.err.println(dataset1.id() + dataset2.id());
        return Lazy.fromClosure { AlignmentSet() }
    }

    @Throws(InvalidConfigurationException::class, DatasetNotFoundException::class, IOException::class)
    fun getStrategy(configuration: String, dataset1: String, dataset2: String): BlockingStrategy {
        val left = getDataset(dataset1)
        val right = getDataset(dataset2)
        return loadConfiguration(configuration).makeBlockingStrategy(getAnalysis(left, right), ExecuteListeners.STDERR)

    }

    @Throws(InvalidConfigurationException::class, DatasetNotFoundException::class)
    fun getLens(configuration: String, dataset1: String, dataset2: String): List<Lens> {
        val left = getDataset(dataset1)
        val right = getDataset(dataset2)
        val combined = CombinedDataset(left, right)
        return loadConfiguration(configuration).makeLenses(combined, getAnalysis(left, right), ExecuteListeners.STDERR)
    }

    @Throws(InvalidConfigurationException::class, DatasetNotFoundException::class)
    fun getGraphFeatures(configuration: String, dataset1 : String, dataset2 : String) : List<GraphFeature> {
        val left = getDataset(dataset1)
        val right = getDataset(dataset2)
        val combined = CombinedDataset(left, right)
        return loadConfiguration(configuration).makeGraphFeatures(combined, getAnalysis(left, right), getPrelinking(left, right), ExecuteListeners.STDERR)
    }

    @Throws(InvalidConfigurationException::class)
    fun getMatcher(configuration : String) : Matcher {
        return loadConfiguration(configuration).makeMatcher()
    }

    @Throws(InvalidConfigurationException::class)
    fun getScorer(configuration: String) : List<Scorer> {
        return loadConfiguration(configuration).makeScorer()
    }

    @Throws(InvalidConfigurationException::class)
    fun getTextFeatures(configuration : String) : List<TextFeature> {
        return loadConfiguration(configuration).makeTextFeatures()
    }

}
