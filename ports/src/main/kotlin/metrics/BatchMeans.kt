package com.group7.metrics

interface BatchMeans {
    val targetBatches: Int

    fun batchCount(): Int

    fun mean(): Double

    fun batchVariance(): Double
}
