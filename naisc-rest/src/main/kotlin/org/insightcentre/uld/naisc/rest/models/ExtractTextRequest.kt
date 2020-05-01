package org.insightcentre.uld.naisc.rest.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.insightcentre.uld.naisc.URIRes

class ExtractTextRequest @JsonCreator constructor (@JsonProperty("entity1") val entity1 : URIRes, @JsonProperty("entity2") val entity2 : URIRes)