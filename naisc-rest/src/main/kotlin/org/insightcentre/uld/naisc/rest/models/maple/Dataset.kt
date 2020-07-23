package org.insightcentre.uld.naisc.rest.models.maple

import com.fasterxml.jackson.annotation.JsonAlias

data class Dataset(
    @JsonAlias("@id")
    val id : String,
    @JsonAlias("@type")
    val type : String,
    val uriSpace : String?,
    val sparqlEndpoint : String?,
    val conformsTo : String?
)