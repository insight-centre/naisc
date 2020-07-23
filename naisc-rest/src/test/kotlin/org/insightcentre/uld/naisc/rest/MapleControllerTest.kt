package org.insightcentre.uld.naisc.rest

import junit.framework.Assert.assertEquals
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.test.JerseyTest
import org.insightcentre.uld.naisc.rest.controllers.MapleController
import org.junit.Test
import java.util.*
import javax.servlet.ServletConfig
import javax.servlet.ServletContext
import javax.ws.rs.core.Application

class MapleControllerTest : JerseyTest() {

    override fun configure(): Application {
        System.setProperty("naisc.skip.install", "yes");
        return org.insightcentre.uld.naisc.rest.Application()
    }

    @Test
    fun testGetServicesMetadata() {
        val response = target("/maple/").request().get()

        assertEquals(200, response.status)

        val content = response.readEntity(String::class.java)
        assert(content.contains("Naisc"))
    }

    @Test
    fun testGetMatchers() {
        val response = target("/maple/matchers").request().get()

        assertEquals(200, response.status)

        val content = response.readEntity(String::class.java)
        assert(content.contains("auto"))
    }
}
