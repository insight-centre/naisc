package org.insightcentre.uld.naisc.rest.models.runs

import org.insightcentre.uld.naisc.NaiscListener
import java.util.*

class Run(val id : String, val left : String, val right : String) {
    var stage : NaiscListener.Stage = NaiscListener.Stage.INITIALIZING
    var message : String? = null
    var start : Date? = null
    var end : Date? = null
}