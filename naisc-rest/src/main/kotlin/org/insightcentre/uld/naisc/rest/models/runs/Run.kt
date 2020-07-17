package org.insightcentre.uld.naisc.rest.models.runs

import org.insightcentre.uld.naisc.*
import org.insightcentre.uld.naisc.main.Configuration
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader
import org.insightcentre.uld.naisc.main.ExecuteListener
import org.insightcentre.uld.naisc.main.Main
import org.insightcentre.uld.naisc.util.None
import java.io.File
import java.util.*

class Run(val id : String, val leftFile : File, val rightFile : File, val config : Configuration) : Runnable {
    var monitor = RunMonitor()
    var start : Date? = null
    var end : Date? = null
    var result : AlignmentSet? = null

    override fun run() {
        start = Date()
        result = Main.execute(id, leftFile, rightFile, config, None(), monitor, DefaultDatasetLoader())
        end = Date()
    }
}

class RunMonitor : ExecuteListener {
    var stage = NaiscListener.Stage.INITIALIZING
    var message =  ""

    override fun updateStatus(stage: NaiscListener.Stage?, message: String?) {
        if(stage != null) this.stage = stage
        if(message != null) this.message = message
    }

    override fun addLensResult(id1: URIRes?, id2: URIRes?, lensId: String?, res: LensResult?) {
        // Ignore
    }

    override fun message(stage: NaiscListener.Stage?, level: NaiscListener.Level?, message: String?) {
        if(stage != null) this.stage = stage
        if(message != null) this.message = message
    }
}