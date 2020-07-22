package org.insightcentre.uld.naisc.rest.models.runs

import org.insightcentre.uld.naisc.Dataset
import org.insightcentre.uld.naisc.main.Configuration
import org.insightcentre.uld.naisc.main.Main
import java.io.File
import java.net.URL

object RunManager {
    var runs = mutableMapOf<String, Run>()

    fun makeDatasetFromURL(url : URL) : Dataset {
        TODO("Unsupported")
    }

    fun makeDatasetFromSPARQL(url : URL, uriSpace : String?, graph : String?) : Dataset {
        TODO("Unsupported")
    }

    fun startRun(id : String, leftFile : Dataset, rightFile : Dataset, config : Configuration) {
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