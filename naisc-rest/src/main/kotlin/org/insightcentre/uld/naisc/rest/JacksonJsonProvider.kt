package org.insightcentre.uld.naisc.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider
import com.fasterxml.jackson.module.kotlin.KotlinModule

import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.ext.Provider

@Provider
@Produces(MediaType.APPLICATION_JSON)
class JacksonJsonProvider : JacksonJaxbJsonProvider() {
    init {

        val objectMapper = ObjectMapper()
                .registerModule(KotlinModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        setMapper(objectMapper)
    }
}
