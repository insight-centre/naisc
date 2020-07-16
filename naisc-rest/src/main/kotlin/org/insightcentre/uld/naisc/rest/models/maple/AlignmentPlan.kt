package org.insightcentre.uld.naisc.rest.models.maple

data class AlignmentPlan(
    val scenarioDefinition: ScenarioDefinition,
    val settings : Object?,
    val matcherDefinition : MatcherDefinition?)

data class MatcherDefinition(
    val id : String,
    val settings : Object?)