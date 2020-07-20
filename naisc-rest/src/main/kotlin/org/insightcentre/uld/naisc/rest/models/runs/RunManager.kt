package org.insightcentre.uld.naisc.rest.models.runs

import org.insightcentre.uld.naisc.main.Configuration
import org.insightcentre.uld.naisc.main.Main
import java.io.File

object RunManager {
    var runs = mutableMapOf<String, Run>()

    fun startRun(id : String, leftFile : File, rightFile : File, config : Configuration) {
        val run = Run(id, leftFile, rightFile, config)
        runs[id] = run
        val thread = Thread(run)
        thread.start()
    }

    fun stopRun(id : String) {
        // Note we don't try to kill the thread, we just make the next message fail so that the run will be aborted
        runs.get(id)?.monitor?.abort()
    }
}