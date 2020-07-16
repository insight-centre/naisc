package org.insightcentre.uld.naisc.rest.models.maple

data class Matcher(
        /** Identifier of the matcher */
        val id: String,
        /** Description of the matcher */
        val description: String,
        /** The settings */
        val settings: Object?)
