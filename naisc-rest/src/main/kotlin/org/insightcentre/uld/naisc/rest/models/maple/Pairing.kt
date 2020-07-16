package org.insightcentre.uld.naisc.rest.models.maple

data class Pairing(
    val score : Double,
    val source : PairingHand,
    val target : PairingHand,
    val synonymizer : Synonymizer?
)

data class PairingHand(
    val lexicalizationSet : String)

data class Synonymizer(
    val lexicon : String,
    val conceptualizationSet : String)
