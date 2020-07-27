package org.insightcentre.uld.naisc.rest.models.runs

import org.insightcentre.uld.naisc.AlignmentSet
import org.insightcentre.uld.naisc.Dataset
import org.insightcentre.uld.naisc.DatasetLoader
import org.insightcentre.uld.naisc.main.Configuration
import org.insightcentre.uld.naisc.main.DefaultDatasetLoader
import org.insightcentre.uld.naisc.main.Main
import org.insightcentre.uld.naisc.main.SPARQLDataset
import java.io.File
import java.net.URL

object RunManager {
    var runs = mutableMapOf<String, Run<*>>()

    fun <D : Dataset> startRun(id : String, leftFile : D, rightFile : D, config : Configuration, loader : DatasetLoader<D>) : Run<D> {
        val run = Run(id, leftFile, rightFile, config, loader)
        runs[id] = run
        val thread = Thread(run)
        thread.start()
        return run
    }

    fun stopRun(id : String) {
        // Note we don't try to kill the thread, we just make the next message fail so that the run will be aborted
        runs.get(id)?.monitor?.abort()
    }

    fun result(id : String) : AlignmentSet? {
        return runs.get(id)?.result
    }

    fun leftURL(id : String) : URL? = runs.get(id)?.leftFile?.location
    fun rightURL(id : String) : URL? = runs.get(id)?.rightFile?.location
}