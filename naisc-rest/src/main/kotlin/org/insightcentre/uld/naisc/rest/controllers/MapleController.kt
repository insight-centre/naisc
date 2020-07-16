package org.insightcentre.uld.naisc.rest.controllers

import org.insightcentre.uld.naisc.rest.models.maple.*
import javax.ws.rs.*
import javax.ws.rs.core.Response

@Path("/maple")
class MapleController {

    @GET
    @Path("/")
    @Produces("application/json")
    fun getServicesMetadata() : ServicesMetadata {
        throw UnsupportedOperationException("TODO")
    }

    @GET
    @Path("/matchers")
    @Produces("application/json")
    fun getMatchers() : List<Matcher> {
        throw UnsupportedOperationException("TODO")
    }

    @POST
    @Path("/matchers/search")
    @Consumes("application/json")
    @Produces("application/json")
    fun searchMatchers(scenarioDefinition : ScenarioDefinition) : Matcher {
        throw UnsupportedOperationException("TODO")
    }

    @GET
    @Path("/matchers/{id}")
    @Produces("application/json")
    fun getMatcherByID(@PathParam("id") id : String) : Matcher {
        throw UnsupportedOperationException("TODO")
    }

    @GET
    @Path("/tasks")
    @Produces("application/json")
    fun getTasks() : Task {
        throw UnsupportedOperationException("TODO")
    }

    @POST
    @Path("/tasks")
    @Consumes("application/json")
    @Produces("application/json")
    fun submitTask(plan : AlignmentPlan) : Task {
        throw UnsupportedOperationException("TODO")
    }

    @GET
    @Path("/tasks/{id}")
    @Produces("application/json")
    fun getTaskByID(@PathParam("id") id : String) : Task {
        throw UnsupportedOperationException("TODO")
    }

    @DELETE
    @Path("/tasks/{id}")
    fun deleteTaskByID(@PathParam("id") id : String) : Response {
        throw UnsupportedOperationException("TODO")
    }

    @GET
    @Path("/tasks/{id}/alignment")
    fun downloadAlignment(@PathParam("id") id : String) : Response {
        throw UnsupportedOperationException("TODO")
    }
}