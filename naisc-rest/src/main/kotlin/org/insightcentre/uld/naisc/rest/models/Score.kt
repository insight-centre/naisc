package org.insightcentre.uld.naisc.rest.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class Score @JsonCreator constructor(@JsonProperty("property") val property : String, @JsonProperty("probability") val probability : Double)
