package com.group7.metrics

/** Batch means interface */
interface BatchMeans {
    /** Number of batches to squash down to when we squash. */
    val targetBatches: Int

    /** Get current number of batches */
    fun batchCount(): Int

    /** Get current number of samples */
    fun sampleCount(): Long

    /** Get current overall mean */
    fun mean(): Double

    /** Get current variance. Most implementations crash when there is only one batch! */
    fun batchVariance(): Double
}
