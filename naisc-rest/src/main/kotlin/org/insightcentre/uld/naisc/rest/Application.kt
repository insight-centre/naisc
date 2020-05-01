package org.insightcentre.uld.naisc.rest

import javax.ws.rs.ApplicationPath
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.ext.Provider

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.glassfish.jersey.server.ResourceConfig

@ApplicationPath("/")
class Application : ResourceConfig() {
    init {
        // Add a package used to scan for components.
        packages(this.javaClass.getPackage().getName() + CONTROLLERS_PACKAGE_PREFIX)
        this.register(JacksonJsonProvider::class.java)
    }

    companion object {

        private val CONTROLLERS_PACKAGE_PREFIX = ".controllers"
    }

}
