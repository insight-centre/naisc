package org.insightcentre.uld.naisc.rest.models.runs

import org.insightcentre.uld.naisc.*
import org.insightcentre.uld.naisc.main.Configuration
import org.insightcentre.uld.naisc.DatasetLoader
import org.insightcentre.uld.naisc.main.ExecuteListener
import org.insightcentre.uld.naisc.main.Main
import org.insightcentre.uld.naisc.util.None
import java.io.File
import java.util.*

class Run<C : Dataset>(val id : String, val leftFile : C, val rightFile : C, val config : Configuration, val loader : DatasetLoader<C>) : Runnable {
    val monitor = RunMonitor()
    var start : Date? = null
    var end : Date? = null
    var result : AlignmentSet? = null

    override fun run() {
        try {
            start = Date()
            result = Main.execute(id, leftFile, rightFile, config, None(), monitor, null, null, loader)
            monitor.updateStatus(NaiscListener.Stage.COMPLETED, "Alignment complete")
        } catch(x : RunAbortedException) {
            System.err.println("Run aborted by user")
        } catch(x : Throwable) {
            x.printStackTrace()
        } finally {
            end = Date()
        }
    }
}

class RunMonitor : ExecuteListener {
    var stage = NaiscListener.Stage.INITIALIZING
    var message =  ""
    var aborted = false

    override fun updateStatus(stage: NaiscListener.Stage?, message: String?) {
        if(aborted) throw RunAbortedException()
        if(stage != null) this.stage = stage
        if(message != null) this.message = message
    }

    override fun addLensResult(id1: URIRes?, id2: URIRes?, lensId: String?, res: LensResult?) {
        if(aborted) throw RunAbortedException()
        // Ignore
    }

    override fun message(stage: NaiscListener.Stage?, level: NaiscListener.Level?, message: String?) {
        if(aborted) throw RunAbortedException()
        if(stage != null) this.stage = stage
        if(message != null) this.message = message
    }

    fun abort() {
        aborted = true
        message = "Run aborted by user"
        stage = NaiscListener.Stage.ABORTED
    }
}

class RunAbortedException : RuntimeException("Run aborted by user")