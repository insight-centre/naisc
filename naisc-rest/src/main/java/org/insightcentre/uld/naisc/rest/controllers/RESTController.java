package org.insightcentre.uld.naisc.rest.controllers;

import org.insightcentre.uld.naisc.Alignment;
import org.insightcentre.uld.naisc.Feature;
import org.insightcentre.uld.naisc.LensResult;
import org.insightcentre.uld.naisc.rest.models.Body;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

@Path("/naisc")
public class RESTController {

    @GET
    @Path("/{config}/block")
    @Produces({ "application/json" })
    public Response block(@QueryParam("left") String left, @QueryParam("right") String right,
                          @PathParam("config") String config, @Context SecurityContext securityContext)
    throws NotFoundException {
        return Response.ok().entity(new ArrayList<>()).build();
    }

    @POST
    @Path("/{config}/extract_text")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response extractText( @PathParam("config") String config, Body body, @Context SecurityContext securityContext)
    throws NotFoundException {
        throw new UnsupportedOperationException("TODO");
    }

    @POST
    @Path("/{config}/graph_features")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response graphFeatures(@PathParam("config") String config, GraphBody body

,@Context SecurityContext securityContext)
    throws NotFoundException {
        throw new UnsupportedOperationException("TODO");
    }

    @POST
    @Path("/{config}/match")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response match(@PathParam("config") String config, List<Alignment> body, @Context SecurityContext securityContext)
    throws NotFoundException {
        throw new UnsupportedOperationException("TODO");
    }

    @POST
    @Path("/{config}/score")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response score(@PathParam("config") String config, List<Feature> body, @Context SecurityContext securityContext)
    throws NotFoundException {
        throw new UnsupportedOperationException("TODO");
    }

    @POST
    @Path("/{config}/text_features")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response textFeatures(@PathParam("config") String config, LensResult body, @Context SecurityContext securityContext)
    throws NotFoundException {
        throw new UnsupportedOperationException("TODO");
    }
    @PUT
    @Path("/upload/{id}")
    @Consumes({ "application/rdf+xml", "text/turtle", "application/n-triples" })
    public Response upload(@PathParam("id") String id, String body, @Context SecurityContext securityContext)
    throws NotFoundException {
        throw new UnsupportedOperationException("TODO");
    }

    private class GraphBody {
    }
}
