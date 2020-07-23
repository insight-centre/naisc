package org.insightcentre.uld.naisc.rest.models.maple

import java.net.URI

data class ServicesMetadata(
    /** The name of the service */
    val service : String,
    /** The version of the service */
    val version : String,
    /** The status of the service */
    val status : Status,
    /** The specifications implemented by the service */
    val specs : List<String>,
    /** An entity to contact for inquiries about the service */
    val contact : Contact?,
    /** The address of the documentation of the service */
    val documentation : String?,
    /** The settings */
    val settings : Any?)

enum class Status(val status : String) {
    starting("starting"), active("active"), busy("busy"), shutting_down("shutting down"), failed("failed")
}

data class Contact(
    val name : String,
    val email : String)

