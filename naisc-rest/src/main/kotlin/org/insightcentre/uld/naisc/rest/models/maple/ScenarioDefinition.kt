package org.insightcentre.uld.naisc.rest.models.maple

data class ScenarioDefinition(
    val leftDataset : Dataset,
    val rightDataset : Dataset,
    val supportDatasets : List<Dataset>,
    val pairings : List<Pairing>)