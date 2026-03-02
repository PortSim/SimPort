package com.group7.metrics

interface BatchMeans {
    val targetBatches: Int

    fun batchCount(): Int

    fun sampleCount(): Long

    fun mean(): Double

    fun batchVariance(): Double
}
