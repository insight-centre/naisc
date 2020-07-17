package org.insightcentre.uld.naisc.rest.controllers

import org.insightcentre.uld.naisc.NaiscListener
import org.insightcentre.uld.naisc.rest.ConfigurationManager
import org.insightcentre.uld.naisc.rest.models.maple.*
import org.insightcentre.uld.naisc.rest.models.runs.Run
import org.insightcentre.uld.naisc.rest.models.runs.RunManager
import java.text.SimpleDateFormat
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.Response

@Path("/maple")
class MapleController {

    @GET
    @Path("/")
    @Produces("application/json")
    fun getServicesMetadata() : ServicesMetadata =
        ServicesMetadata(
            "Naisc",
            "1.1",
            Status.active,
            listOf("http://art.uniroma2.it/maple/alignment-services-1.0.yaml"), null, null, null)
    @GET
    @Path("/matchers")
    @Produces("application/json")
    fun getMatchers() : List<Matcher> = ConfigurationManager.getAllConfigurations().map { c -> Matcher(c._1, c._2.description, null) }

    @POST
    @Path("/matchers/search")
    @Consumes("application/json")
    @Produces("application/json")
    fun searchMatchers(scenarioDefinition : ScenarioDefinition) : Matcher = Matcher("auto", "Search not supported, use `auto` as this works for everything", null)

    @GET
    @Path("/matchers/{id}")
    @Produces("application/json")
    fun getMatcherByID(@PathParam("id") id : String) : Matcher {
        val config = ConfigurationManager.loadConfiguration(id)
        return Matcher(id, config.description, null)
    }

    @GET
    @Path("/tasks")
    @Produces("application/json")
    fun getTasks() : List<Task> = RunManager.runs.values.map { r -> run2task(r) }

    private fun run2task(r : Run) = Task(r.id, r.leftFile.name, r.rightFile.name, convertStatus(r.monitor.stage), null,
            Reason(r.monitor.message ?: ""), convertTime(r.start), convertTime(r.start), convertTime(r.end))

    private fun convertTime(start: Date?): String? {
        if(start != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            return sdf.format(start)
        } else {
            return null
        }
    }


    private fun convertStatus(stage: NaiscListener.Stage): Status {
        return when(stage) {
            NaiscListener.Stage.FAILED -> Status.failed
            NaiscListener.Stage.INITIALIZING -> Status.starting
            else -> Status.active
        }
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
    fun getTaskByID(@PathParam("id") id : String) : Response {
        val run = RunManager.runs[id]
        if(run != null) {
            return Response.status(Response.Status.OK).entity(run2task(run)).build()
        } else {
            return Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @DELETE
    @Path("/tasks/{id}")
    fun deleteTaskByID(@PathParam("id") id : String) : Response {
        RunManager.stopRun(id)
        return Response.status(Response.Status.NO_CONTENT).build()
    }

    @GET
    @Path("/tasks/{id}/alignment")
    fun downloadAlignment(@PathParam("id") id : String) : Response {
        throw UnsupportedOperationException("TODO")
    }
}