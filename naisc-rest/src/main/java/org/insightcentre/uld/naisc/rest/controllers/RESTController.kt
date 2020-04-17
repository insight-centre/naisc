package org.insightcentre.uld.naisc.rest.controllers

import org.insightcentre.uld.naisc.*
import org.insightcentre.uld.naisc.rest.ConfigurationManager
import org.insightcentre.uld.naisc.rest.models.ExtractTextRequest

import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

@Path("/naisc")
class RESTController {

    @GET
    @Path("/{config}/block")
    @Produces("application/json")
    @Throws(NotFoundException::class)
    fun block(@QueryParam("left") left: String, @QueryParam("right") right: String,
              @PathParam("config") config: String, @Context @Suppress("UNUSED_PARAMETER") securityContext: SecurityContext): Response {
        try {
            val blocks = ConfigurationManager.getStrategy(config, left, right).block(ConfigurationManager.getDataset(left),
                    ConfigurationManager.getDataset(right))
            return Response.ok().entity(blocks).build()
        } catch (x: Exception) {
            return Response.status(500).entity(x.message).build()
        }

    }

    @POST
    @Path("/{config}/extract_text")
    @Consumes("application/json")
    @Produces("application/json")
    @Throws(NotFoundException::class)
    fun extractText(@PathParam("config") config: String, body: ExtractTextRequest, @Context @Suppress("UNUSED_PARAMETER") securityContext: SecurityContext): Response {
        try {
            val lenses = ConfigurationManager.getLens(config, body.entity1.dataset, body.entity2.dataset)
            var result = mutableListOf<LensResult>()
            for(lens in lenses) {
                val r = lens.extract(body.entity1.toJena(ConfigurationManager.getDataset(body.entity1.dataset)),
                    body.entity2.toJena(ConfigurationManager.getDataset(body.entity2.dataset)))
                if(r.has()) {
                    result.add(r.get())
                }
            }
            return Response.ok().entity(result).build()
        } catch(x : Exception) {
            return Response.status(500).entity(x.message).build()
        }
    }

    @POST
    @Path("/{config}/graph_features")
    @Consumes("application/json")
    @Produces("application/json")
    @Throws(NotFoundException::class)
    fun graphFeatures(@PathParam("config") config: String, body: ExtractTextRequest, @Context @Suppress("UNUSED_PARAMETER") securityContext: SecurityContext): Response {
        try {
            val feats = ConfigurationManager.getGraphFeatures(config, body.entity1.dataset, body.entity2.dataset)
            var result = mutableListOf<Feature>()
            for(feat in feats) {
                result.addAll(feat.extractFeatures(body.entity1.toJena(ConfigurationManager.getDataset(body.entity1.dataset)),
                    body.entity2.toJena(ConfigurationManager.getDataset(body.entity2.dataset))))
            }
            return Response.ok().entity(result).build()
        } catch(x : Exception) {
            return Response.status(500).entity(x.message).build()
        }

    }

    @POST
    @Path("/{config}/match")
    @Consumes("application/json")
    @Produces("application/json")
    @Throws(NotFoundException::class)
    fun match(@PathParam("config") config: String, body: List<Alignment>, @Context @Suppress("UNUSED_PARAMETER") securityContext: SecurityContext): Response {
        try {
            val matcher = ConfigurationManager.getMatcher(config)
            return Response.ok().entity(matcher.align(AlignmentSet(body))).build()
        } catch(x : Exception) {
            return Response.status(500).entity(x.message).build()
        }
    }

    @POST
    @Path("/{config}/score")
    @Consumes("application/json")
    @Produces("application/json")
    @Throws(NotFoundException::class)
    fun score(@PathParam("config") config: String, body: List<Feature>, @Context @Suppress("UNUSED_PARAMETER") securityContext: SecurityContext): Response {
        try {
            val scorers = ConfigurationManager.getScorer(config)
            val features = FeatureSet(body)
            return Response.ok().entity(scorers.map { s -> { s.similarity(features) } }).build()
        } catch(x : Exception) {
            return Response.status(500).entity(x.message).build()
        }
    }

    @POST
    @Path("/{config}/text_features")
    @Consumes("application/json")
    @Produces("application/json")
    @Throws(NotFoundException::class)
    fun textFeatures(@PathParam("config") config: String, body: LensResult, @Context @Suppress("UNUSED_PARAMETER") securityContext: SecurityContext): Response {
        try {
            val scorers = ConfigurationManager.getTextFeatures(config)
            return Response.ok().entity(scorers.flatMap { s -> s.extractFeatures(body).toList() }).build()
        } catch(x : Exception) {
            return Response.status(500).entity(x.message).build()
        }
    }

    @PUT
    @Path("/upload/{id}")
    @Consumes("application/rdf+xml", "text/turtle", "application/n-triples")
    @Throws(NotFoundException::class)
    fun upload(@PathParam("id") id: String, body: String, @Context @Suppress("UNUSED_PARAMETER") securityContext: SecurityContext): Response {
        try {
            ConfigurationManager.loadDataset(id, body)
            return Response.ok().build()
        } catch(x : Exception) {
            return Response.status(500).entity(x.message).build()
        }
    }

    @POST
    @Path("/{config}/prematch")
    @Consumes("application/json")
    @Produces("application/json")
    @Throws(NotFoundException::class)
    fun prematch(@PathParam("config") config : String, body: Blocking) : Response {
        throw UnsupportedOperationException("TODO")
    }
}
