package org.insightcentre.uld.naisc.rest.models.maple

data class Task(
        /** Identifier of the task */
        val id : String,
        /** The dataset containing the left-hand side of the semantic correspondences to compute */
        val leftDataset: String,
        /** The dataset containing the right-hand side of the semantic correspondences to compute */
        val rightDataset : String,
        /** Different stages of a task lifecycle */
        val status: Status,
        /** the percentage of work done by `running` tasks */
        val progress: Int?,
        /** the reason of a failure */
        val reason: Reason?,
        /** The instant at which the task was submitted (ISO 8601 String) */
        val submissionTime: String?,
        /** The instant at which the execution of the task actually started */
        val startTime: String?,
        /** The instant at which the execution of the task ended, because of successful completion or a failure */
        val endTime: String?
)
data class Reason (
        val message : String)